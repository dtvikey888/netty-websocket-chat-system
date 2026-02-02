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
                                                ctx.channel().attr(CLIENT_WEBSOCKET_URI).set(uri);
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

                                            // 解析纯净客服ID
                                            Channel channel = ctx.channel();
                                            String uri = channel.attr(NettyWebSocketServer.CLIENT_WEBSOCKET_URI).get();
                                            if (uri != null && uri.startsWith(webSocketBasePath + "/")) {
                                                String csReceiverId = uri.substring((webSocketBasePath + "/").length());
                                                // 去除查询参数，避免ID污染
                                                if (csReceiverId.contains("?")) {
                                                    csReceiverId = csReceiverId.split("\\?")[0];
                                                }
                                                // 注册到业务映射表，供后续消息转发使用
                                                channel.attr(NettyConstant.SESSION_ID_KEY).set(csReceiverId);
                                                channel.attr(NettyConstant.RECEIVER_ID_KEY).set(csReceiverId); // 新增：绑定receiverId，解决纯文本为空问题
                                                channel.attr(NettyConstant.SENDER_TYPE_KEY).set(WebSocketMsgVO.SENDER_TYPE_CS); // 新增：绑定发送者类型为客服
                                                channel.attr(NettyConstant.USER_ID_KEY).set(csReceiverId); // 新增：绑定userId，避免纯文本消息userId为null
                                                NettyWebSocketServerHandler.RECEIVER_CHANNEL_MAP.put(csReceiverId, channel);
                                                log.info("【会话注册成功】通道ID：{}，客服ID：{}，已加入在线映射表", channelId, csReceiverId);
                                                // 新增：查询并推送该客服的离线消息
                                                OfflineMsgQueryParam queryParam = new OfflineMsgQueryParam();
                                                queryParam.setServiceStaffId(csReceiverId);
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
                                                    offlineMsgService.markOfflineMsgAsPushed(csReceiverId);
                                                    log.info("【离线消息补偿推送成功】客服：{}，共推送{}条离线消息", csReceiverId, offlineMsgList.size());
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