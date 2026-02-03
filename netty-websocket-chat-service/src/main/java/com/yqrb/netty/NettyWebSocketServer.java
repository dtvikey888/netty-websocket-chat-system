package com.yqrb.netty;

import com.alibaba.fastjson.JSON;
import com.yqrb.netty.constant.NettyConstant;
import com.yqrb.pojo.OfflineMsg;
import com.yqrb.pojo.query.OfflineMsgQueryParam;
import com.yqrb.pojo.vo.OfflineMsgVO;
import com.yqrb.pojo.vo.WebSocketMsgVO;
import com.yqrb.service.OfflineMsgService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户/客服 → 连接/newspaper/websocket?携带标识（chatType=PRE_SALE+preSaleSessionId+userId/csId）→ Netty WebSocket服务器
 * → 解析标识，区分「售前/售后」→ 售前消息 → 调用PreSaleChatMessageService → 存入pre_sale_chat_message表 → 实时推送给对应接收方（机器人/专属客服/用户）
 * → 售后消息 → 沿用现有逻辑 → 存入chat_message表 + session_mapping表
 * 1. 用户侧连接 WebSocket（售前咨询） 前端连接 URL 示例（携带售前标识、售前会话 ID、用户 ID）：
 * chatType=PRE_SALE：核心标识，告诉 WebSocket 服务器这是售前咨询连接（售后可传chatType=AFTER_SALE，不传默认按售后处理）；
 * preSaleSessionId：售前专属会话 ID（前端调用之前的/api/pre-sale/chat/generate-session-id接口获取）；
 * userId：用户 ID（与售前表的senderId对应，用于存储记录）。
 * ws://localhost:8088/newspaper/websocket?chatType=PRE_SALE&preSaleSessionId=PRE_SESSION_3e8a7c9d4b5f467a890abcdef12345678&userId=LYQY_USER_5fbb6357b77d2e6436a46336
 * 2. 售前客服侧连接 WebSocket（人工对接） 前端连接 URL 示例（携带售前标识、客服 ID）：
 * chatType=PRE_SALE：标识售前连接；
 * csId：客服 ID（用于绑定用户会话，推送用户消息给对应客服）。
 * ws://localhost:8088/newspaper/websocket?chatType=PRE_SALE&csId=LYQY_CS_5fc5bff4b77d2e6436a618aa
 *
 */
@Component
public class NettyWebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(NettyWebSocketServer.class);

    @Value("${custom.netty.websocket.port:8081}")
    private int port;
    @Value("${custom.netty.websocket.boss-thread-count:1}")
    private int bossThreadCount;
    @Value("${custom.netty.websocket.worker-thread-count:0}")
    private int workerThreadCount;
    @Value("${custom.netty.websocket.idle-timeout:30}")
    private int idleTimeout;

    // 注入OfflineMsgService
    @Autowired
    private OfflineMsgService offlineMsgService;

    public static final AttributeKey<String> CLIENT_WEBSOCKET_URI = AttributeKey.valueOf("CLIENT_WEBSOCKET_URI");

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture serverFuture;

    @PostConstruct
    public void start() {
        new Thread(() -> {
            bossGroup = new NioEventLoopGroup(bossThreadCount);
            workerGroup = workerThreadCount > 0 ? new NioEventLoopGroup(workerThreadCount) : new NioEventLoopGroup();

            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, 1024)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline pipeline = ch.pipeline();
                                String channelId = ch.id().asShortText();
                                log.info("【通道初始化】通道ID：{}", channelId);

                                // ===== 1. HTTP基础处理器（必须最先加）=====
                                pipeline.addLast(new HttpServerCodec()); // HTTP编解码
                                pipeline.addLast(new ChunkedWriteHandler()); // 大文件支持
                                pipeline.addLast(new HttpObjectAggregator(64 * 1024 * 1024)); // 64MB，确保完整聚合HTTP请求

                                // ===== 2. 捕获客户端URI（必须在协议升级前，已优化：先传递消息再移除）=====
                                pipeline.addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        if (msg instanceof io.netty.handler.codec.http.HttpRequest) {
                                            io.netty.handler.codec.http.HttpRequest request = (io.netty.handler.codec.http.HttpRequest) msg;
                                            // 仅处理GET请求（WebSocket握手是GET请求）
                                            if (request.method().equals(io.netty.handler.codec.http.HttpMethod.GET)) {
                                                String uri = request.uri();
                                                ctx.channel().attr(NettyWebSocketServer.CLIENT_WEBSOCKET_URI).set(uri);
                                                log.info("【URI捕获】通道ID：{}，URI：{}，请求类型：{}",
                                                        channelId, uri, msg.getClass().getSimpleName());

                                                // 先传递消息，再移除自身Handler（确保请求不被阻断）
                                                super.channelRead(ctx, msg);

                                                // 移除当前Handler，避免重复处理
                                                ctx.pipeline().remove(this);
                                                return;
                                            }
                                        }
                                        // 非GET请求/非HttpRequest，直接传递
                                        super.channelRead(ctx, msg);
                                    }
                                });

                                // ===== 3. 心跳检测（已优化：放在WebSocket协议处理器之前）=====
                                pipeline.addLast(new IdleStateHandler(idleTimeout, 0, 0, TimeUnit.SECONDS));

                                // ===== 4. WebSocket 协议升级（核心！修改包装方式，让处理器真正生效）=====
                                String webSocketBasePath = "/newspaper/websocket";
// 1. 直接创建并添加 WebSocketServerProtocolHandler 到流水线（关键：让其自动加载子处理器）
                                WebSocketServerProtocolHandler wsProtocolHandler = new WebSocketServerProtocolHandler(
                                        webSocketBasePath,  // 核心前缀路径
                                        null,               // 子协议（无则为null）
                                        true,               // 允许扩展
                                        1024 * 1024,        // 最大帧大小
                                        false,              // 不允许关闭帧延迟
                                        true                // 关键：忽略路径中的查询参数和后缀
                                );
                                pipeline.addLast("webSocketProtocolHandler", wsProtocolHandler);

                                // 2. 独立添加事件监听器，捕获 HANDSHAKE_COMPLETE 事件（不包装，直接监听，更稳定）
                                pipeline.addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                        // 仅捕获握手完成事件
                                        if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                                            String channelId = ctx.channel().id().asShortText();
                                            log.info("【协议升级】通道ID：{}，WebSocket握手成功", channelId);

                                            // 解析纯净ID（通用化：支持用户/客服，不再限定为客服）
                                            Channel channel = ctx.channel();
                                            String uri = channel.attr(NettyWebSocketServer.CLIENT_WEBSOCKET_URI).get();
                                            if (uri != null && uri.startsWith(webSocketBasePath + "/")) {
                                                String receiverId = uri.substring((webSocketBasePath + "/").length());
                                                // 去除查询参数，避免ID污染
                                                if (receiverId.contains("?")) {
                                                    receiverId = receiverId.split("\\?")[0];
                                                }

                                                // ======================================
                                                // 核心修改：根据ID前缀区分用户/客服，绑定对应的senderType
                                                // 约定：用户ID前缀 LYQY_USER_，客服ID前缀 LYQY_CS_
                                                String senderType;
                                                if (receiverId.startsWith("LYQY_USER_")) {
                                                    // 用户连接：绑定为USER
                                                    senderType = WebSocketMsgVO.SENDER_TYPE_USER;
                                                } else if (receiverId.startsWith("LYQY_CS_")) {
                                                    // 客服连接：绑定为CS
                                                    senderType = WebSocketMsgVO.SENDER_TYPE_CS;
                                                } else {
                                                    // 默认：未知类型，绑定为USER（可根据业务调整）
                                                    senderType = WebSocketMsgVO.SENDER_TYPE_USER;
                                                    log.warn("【会话注册警告】通道ID：{}，ID前缀未匹配（非LYQY_USER_/LYQY_CS_），默认绑定为USER类型，ID：{}", channelId, receiverId);
                                                }
                                                // ======================================

                                                // 注册到业务映射表，供后续消息转发使用
                                                channel.attr(NettyConstant.SESSION_ID_KEY).set(receiverId);
                                                channel.attr(NettyConstant.RECEIVER_ID_KEY).set(receiverId);
                                                // 替换原来的强制CS绑定，使用区分后的senderType
                                                channel.attr(NettyConstant.SENDER_TYPE_KEY).set(senderType);
                                                channel.attr(NettyConstant.USER_ID_KEY).set(receiverId);
                                                NettyWebSocketServerHandler.RECEIVER_CHANNEL_MAP.put(receiverId, channel);

                                                // 优化日志：打印连接类型
                                                log.info("【会话注册成功】通道ID：{}，ID：{}，连接类型：{}，已加入在线映射表",
                                                        channelId, receiverId, senderType);

                                                // 新增：查询并推送该ID的离线消息（兼容用户/客服，保留原有逻辑）
                                                OfflineMsgQueryParam queryParam = new OfflineMsgQueryParam();
                                                queryParam.setServiceStaffId(receiverId);
                                                queryParam.setIsPushed(0);
                                                List<OfflineMsgVO> offlineMsgList = offlineMsgService.getOfflineMsgList(queryParam);

                                                if (!CollectionUtils.isEmpty(offlineMsgList)) {
                                                    for (OfflineMsgVO offlineMsgVO : offlineMsgList) {
                                                        // 构建WebSocket消息并推送
                                                        WebSocketMsgVO wsMsg = new WebSocketMsgVO();
                                                        wsMsg.setMsgType(offlineMsgVO.getMsgType());
                                                        wsMsg.setMsgContent(offlineMsgVO.getMsgContent());
                                                        wsMsg.setReceiverId(offlineMsgVO.getServiceStaffId());
                                                        channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(wsMsg)));
                                                    }
                                                    // 标记为已推送
                                                    offlineMsgService.markOfflineMsgAsPushed(receiverId);
                                                    log.info("【离线消息补偿推送成功】ID：{}，连接类型：{}，共推送{}条离线消息",
                                                            receiverId, senderType, offlineMsgList.size());
                                                }
                                            } else {
                                                log.error("【会话注册失败】通道ID：{}，URI格式错误：{}", channelId, uri);
                                            }

                                            // 优化：清理无用HTTP处理器（添加存在性判断+异常捕获，避免NoSuchElementException）
                                            try {
                                                if (ctx.pipeline().get(HttpServerCodec.class) != null) {
                                                    ctx.pipeline().remove(HttpServerCodec.class);
                                                } else {
                                                    log.warn("【处理器清理】通道ID：{}，HttpServerCodec 已不存在，无需移除", channelId);
                                                }

                                                if (ctx.pipeline().get(HttpObjectAggregator.class) != null) {
                                                    ctx.pipeline().remove(HttpObjectAggregator.class);
                                                } else {
                                                    log.warn("【处理器清理】通道ID：{}，HttpObjectAggregator 已不存在，无需移除", channelId);
                                                }

                                                if (ctx.pipeline().get(ChunkedWriteHandler.class) != null) {
                                                    ctx.pipeline().remove(ChunkedWriteHandler.class);
                                                } else {
                                                    log.warn("【处理器清理】通道ID：{}，ChunkedWriteHandler 已不存在，无需移除", channelId);
                                                }
                                            } catch (Exception e) {
                                                log.warn("【处理器清理】通道ID：{}，清理HTTP处理器出现意外异常，不影响后续通信", channelId, e);
                                            }

                                            // 移除当前事件监听器（仅需执行一次，避免重复监听）
                                            ctx.pipeline().remove(this);
                                            return;
                                        }

                                        // 其他事件，正常传递
                                        super.userEventTriggered(ctx, evt);
                                    }
                                });


                                // ===== 5. 自定义编解码器（必须在协议升级后）=====
                                pipeline.addLast(new WebSocketMsgCodec());

                                // ===== 6. 业务处理器（最后加，已优化完成）=====
                                pipeline.addLast(new NettyWebSocketServerHandler());

                                log.info("【通道初始化完成】通道ID：{}，处理器链路：{}", channelId, pipeline.names());
                            }
                        });

                // 绑定端口
                serverFuture = bootstrap.bind(port).sync();
                log.info("=====================================");
                log.info("Netty WebSocket服务启动成功");
                log.info("端口：{}", port);
                log.info("WebSocket路径：/newspaper/websocket");
                log.info("=====================================");

                // 阻塞等待服务关闭
                serverFuture.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Netty服务启动失败：{}", e.getMessage(), e);
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
                log.info("Netty EventLoopGroup 已优雅关闭");
            }
        }, "Netty-WebSocket-Server-Thread").start();
    }

    @PreDestroy
    public void stop() {
        if (serverFuture != null) {
            serverFuture.channel().close().syncUninterruptibly();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("Netty WebSocket服务已关闭");
    }
}