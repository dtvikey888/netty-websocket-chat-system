package com.yqrb.netty;

import com.alibaba.fastjson.JSON;
import com.yqrb.netty.constant.NettyConstant;
import com.yqrb.pojo.query.OfflineMsgQueryParam;
import com.yqrb.pojo.vo.ChatMessageVO;
import com.yqrb.pojo.vo.OfflineMsgVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.pojo.vo.WebSocketMsgVO;
import com.yqrb.service.ChatMessageService;
import com.yqrb.service.OfflineMsgService;
import com.yqrb.util.SpringContextUtil;
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
 * 用户侧（receiverId以LYQY_USER_开头）
 * ws://localhost:8088/newspaper/websocket/LYQY_USER_5fbb6357b77d2e6436a46336?sessionId=SESSION_8600d39e8ae844828c9d4bb17ed118ac
 * 客服侧（receiverId以LYQY_CS_开头）
 * ws://localhost:8088/newspaper/websocket/LYQY_CS_5fc5bff4b77d2e6436a618aa?sessionId=SESSION_8600d39e8ae844828c9d4bb17ed118ac
 客服能收到这条离线消息（网络层面消息已推送到前端），但前端几乎无法将消息正确展示到对应会话中，会出现 ** 消息 “无归属 / 飘错窗口”** 的问题 —— 这是当前代码的核心问题：后端只做了「通道级推送」（只要客服在线就推），但没有和前端「会话激活状态」联动，前端缺少 sessionId 的匹配上下文。
 一、先讲清楚：为什么 “能收到但展示异常”？
 1. 后端层面：消息 100% 推送成功（客服在线 = 有可用通道）
 当前 Netty 的逻辑是：只要客服的 receiverId 能匹配到活跃的 WebSocket 通道，就直接通过channel.writeAndFlush把消息写入通道。
 客服在线时，RECEIVER_CHANNEL_MAP中会有该客服 ID 对应的唯一通道，所以网络层面消息一定会发送到前端，后端日志会打印「离线消息补偿推送成功」。
 2. 前端层面：消息无法正确渲染（无 sessionId 会话上下文）
 客服在线但 “没选择某个 sessionId”，本质是前端处于「多会话状态」，但未激活该 sessionId 对应的会话窗口（比如客服在会话 A 的窗口，而消息是会话 B 的）。
 前端接收 WebSocket 消息的核心逻辑是按 sessionId 匹配会话容器：如果前端没有为该 sessionId 创建会话窗口、或未激活该窗口，即使收到了包含 sessionId 的消息，也没有对应的 DOM 容器渲染这条消息，最终表现为：
 消息飘到「默认会话窗口」；
 消息直接被前端忽略（无匹配 sessionId 则不渲染）；
 只看到消息提示，但点不开对应会话（未加载）。
 简单说：后端把 “带 sessionId 的消息” 送到了前端，但前端没地方放这条消息。
 二、当前代码的核心问题：推送逻辑是「通道级」，而非「会话级」
 当前离线消息推送只判断 **「客服是否在线」（通道是否存在），但完全不关心「前端当前激活的是哪个 sessionId 会话」—— 因为前端的会话选择 / 激活状态，是前端的本地状态，后端目前没有感知 **。
 这种设计在「单会话场景」下没问题，但在 ** 客服多会话（多 sessionId）** 的核心场景下，就会出现你说的 “消息推了但没意义” 的问题。
 三、解决方案：分「轻量适配（推荐）」和「规范方案」，按需选择
 核心原则：后端只负责把「带完整 sessionId 的消息」推给前端，由前端处理「按 sessionId 路由 + 未读角标」（这是行业通用方案，因为会话激活状态属于前端交互层，不适合放到后端管理），后端无需感知前端的会话状态，最小化前后端耦合。
 方案 1：轻量适配（前端改造，后端无代码修改，最快落地）
 这是成本最低、最推荐的方案，利用当前消息中已有的 sessionId 字段，让前端做 2 个关键逻辑改造，即可彻底解决问题：
 按 sessionId 做消息路由：前端接收 WebSocket 消息时，先解析sessionId，根据sessionId匹配对应的会话窗口：
 如果该 sessionId 的会话窗口已打开→直接渲染到该窗口的消息列表；
 如果该 sessionId 的会话窗口未打开→创建会话窗口（或标记为未读），并在会话列表显示「未读消息数」。
 会话列表未读角标：对未激活会话的消息，在前端会话列表为该 sessionId 添加未读消息数字角标，提醒客服有新消息，客服点击该会话时再加载 / 渲染消息。
 ✅ 优势：后端代码完全不用改（当前消息已经携带sessionId），只改前端，快速解决问题，符合前后端分离的设计思想。

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
                                                // 新增：核心校验（售后专属！receiverId必须以LYQY_USER_/LYQY_CS_开头）
                                                // 拦截错误的receiverId，避免后续拼接/日志/业务逻辑出错
                                                if (!receiverId.startsWith("LYQY_USER_") && !receiverId.startsWith("LYQY_CS_")) {
                                                    log.error("【会话注册失败】通道ID：{}，售后receiverId格式错误！必须以LYQY_USER_/LYQY_CS_开头，当前错误receiverId：{}，请检查前端WebSocket URL", channelId, receiverId);
                                                    ctx.channel().close(); // 直接关闭通道，避免无效连接
                                                    return;
                                                }
                                                // ======================================

                                                // ===== 新增：解析前端传入的sessionId（业务会话ID）+ 基础校验 =====
                                                String sessionId = parseSessionIdFromUri(uri);
                                                if (sessionId == null || sessionId.trim().isEmpty()) {
                                                    log.error("【会话注册失败】通道ID：{}，未传入sessionId参数，URI：{}", channelId, uri);
                                                    ctx.channel().close(); // 售后必须传sessionId，不传直接关闭通道
                                                    return;
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
                                                channel.attr(NettyConstant.SESSION_ID_KEY).set(sessionId);
                                                channel.attr(NettyConstant.RECEIVER_ID_KEY).set(receiverId);
                                                // 替换原来的强制CS绑定，使用区分后的senderType
                                                channel.attr(NettyConstant.SENDER_TYPE_KEY).set(senderType);
                                                channel.attr(NettyConstant.USER_ID_KEY).set(receiverId);
                                                NettyWebSocketServerHandler.RECEIVER_CHANNEL_MAP.put(receiverId, channel);

                                                // 优化日志：打印sessionId，便于调试（可选，建议加）
                                                log.info("【会话注册成功】通道ID：{}，ID：{}，连接类型：{}，业务sessionId：{}，已加入在线映射表",
                                                        channelId, receiverId, senderType, sessionId);

                                                // ======================================
                                                // 【新增核心代码】查询chat_message未读消息并推送
                                                // ======================================
                                                try {
                                                    // 1. 通过SpringContextUtil获取ChatMessageService Bean（Netty非Spring管理，需手动获取）
                                                    ChatMessageService chatMessageService = SpringContextUtil.getBean(ChatMessageService.class);

                                                    String authReceiverId = "R_FIXED_0000_"+receiverId;
                                                    String authSessionId = channel.attr(NettyConstant.SESSION_ID_KEY).get();
                                                    // 修正日志名称，避免误导
                                                    log.info("【未读消息推送准备】拼接后的authReceiverId：{}，业务sessionId：{}，原始发送者ID：{}", authReceiverId, authSessionId, receiverId);

                                                    // 2. 调用方法查询该接收方的未读消息
                                                    Result<List<ChatMessageVO>> unreadResult = chatMessageService.getUnreadMessageListBySessionId(authSessionId, authReceiverId);

                                                    // 3. 校验查询结果，非空才推送
                                                    if (unreadResult != null && unreadResult.isSuccess() && !CollectionUtils.isEmpty(unreadResult.getData())) {
                                                        List<ChatMessageVO> unreadMsgList = unreadResult.getData();
                                                        log.info("【未读消息推送准备】ID：{}，连接类型：{}，找到{}条chat_message未读消息",
                                                                receiverId, senderType, unreadMsgList.size());

                                                        // 4. 遍历封装为WebSocketMsgVO，推送给当前通道
                                                        for (ChatMessageVO unreadMsg : unreadMsgList) {
                                                            WebSocketMsgVO wsMsg = new WebSocketMsgVO();
                                                            wsMsg.setReceiverId(unreadMsg.getReceiverId()); // 接收方ID（自身）
                                                            wsMsg.setUserId(unreadMsg.getSenderId()); // 发送方ID（消息原发送者）
                                                            wsMsg.setMsgContent(unreadMsg.getContent()); // 消息内容
                                                            wsMsg.setMsgType(unreadMsg.getMsgType()); // 消息类型
                                                            wsMsg.setSessionId(unreadMsg.getSessionId()); // 会话ID
                                                            wsMsg.setSendTime(unreadMsg.getSendTime()); // 消息发送时间
                                                            wsMsg.setSenderType(unreadMsg.getSenderType()); // 发送者类型（USER/CS/SYSTEM）

                                                            // 5. 写入通道推送消息（和现有离线消息推送写法一致）
                                                            String jsonMsg = JSON.toJSONString(wsMsg);
                                                            channel.writeAndFlush(new TextWebSocketFrame(jsonMsg));
                                                        }

                                                        log.info("【未读消息推送完成】ID：{}，连接类型：{}，成功推送{}条chat_message未读消息",
                                                                receiverId, senderType, unreadMsgList.size());
                                                    } else {
                                                        log.info("【未读消息查询】ID：{}，无chat_message未读消息", receiverId);
                                                    }
                                                } catch (Exception e) {
                                                    log.error("【未读消息推送异常】ID：{}，异常信息：{}", receiverId, e.getMessage(), e);
                                                    // 异常不阻断后续流程，仅打印日志
                                                }

                                                // ======================================
                                                // 【改造后】离线消息（offline_msg）指定会话精准推送逻辑
                                                // 核心：绑定业务sessionId，按「接收者ID+sessionId+未推送」精准查询/推送/标记
                                                // ======================================
                                                // 1. 从通道获取绑定的业务sessionId（和chat_message未读消息用同一个，保证会话统一）
                                                String bindSessionId = channel.attr(NettyConstant.SESSION_ID_KEY).get();
                                                // 2. 构建查询参数：新增sessionId过滤，仅查当前会话的未推送离线消息
                                                OfflineMsgQueryParam queryParam = new OfflineMsgQueryParam();
                                                queryParam.setServiceStaffId(receiverId); // 接收者ID（用户/客服）
                                                queryParam.setSessionId(bindSessionId);   // 核心：指定业务会话ID，实现精准查询
                                                queryParam.setIsPushed(0);                // 仅查询未推送的消息
                                                List<OfflineMsgVO> offlineMsgList = offlineMsgService.getOfflineMsgList(queryParam);

                                                if (!CollectionUtils.isEmpty(offlineMsgList)) {
                                                    for (OfflineMsgVO offlineMsgVO : offlineMsgList) {
                                                        // 构建WebSocket消息并推送：新增设置sessionId，前端可识别所属会话
                                                        WebSocketMsgVO wsMsg = new WebSocketMsgVO();
                                                        wsMsg.setMsgType(offlineMsgVO.getMsgType());
                                                        wsMsg.setMsgContent(offlineMsgVO.getMsgContent());
                                                        wsMsg.setReceiverId(offlineMsgVO.getServiceStaffId());
                                                        wsMsg.setSessionId(offlineMsgVO.getSessionId()); // 关键：传递会话ID给前端
                                                        channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(wsMsg)));
                                                    }
                                                    // 3. 标记已推送：调用带「接收者ID+sessionId」的方法，精准标记当前会话的消息
                                                    offlineMsgService.markOfflineMsgAsPushed(receiverId, bindSessionId);
                                                    // 优化日志：添加sessionId，便于调试和问题排查
                                                    log.info("【离线消息补偿推送成功】ID：{}，连接类型：{}，会话ID：{}，共推送{}条离线消息",
                                                            receiverId, senderType, bindSessionId, offlineMsgList.size());
                                                } else {
                                                    // 无消息时也打印sessionId，日志更完整
                                                    log.info("【离线消息查询】ID：{}，连接类型：{}，会话ID：{}，无offline_msg未读消息",
                                                            receiverId, senderType, bindSessionId);
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

    // 新增：极简解析URI中query参数里的sessionId（只解析sessionId，最小化改动）
    private String parseSessionIdFromUri(String uri) {
        if (uri == null || !uri.contains("?sessionId=")) {
            return null;
        }
        // 截取?后的部分，按&分割，找到sessionId=xxx的片段
        String paramPart = uri.substring(uri.indexOf("?") + 1);
        String[] paramPairs = paramPart.split("&");
        for (String pair : paramPairs) {
            if (pair.startsWith("sessionId=")) {
                return pair.substring("sessionId=".length()).trim();
            }
        }
        return null;
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