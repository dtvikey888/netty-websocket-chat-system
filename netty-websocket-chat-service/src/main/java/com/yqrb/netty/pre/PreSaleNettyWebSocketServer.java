package com.yqrb.netty.pre;

import com.alibaba.fastjson.JSON;
import com.yqrb.netty.constant.NettyConstant;
import com.yqrb.pojo.po.PreSaleChatMessagePO;
import com.yqrb.pojo.vo.PreSaleChatMessageVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.PreSaleChatMessageService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 售前Netty WebSocket服务（完全独立，端口8089）
 * 优化：对齐售后的未读消息推送逻辑，处理ReceiverId前缀
 */
@Component
public class PreSaleNettyWebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(PreSaleNettyWebSocketServer.class);

    // 售前Netty配置（从application.yml读取，独立配置）
    @Value("${custom.netty.pre-sale.websocket.port:8089}")
    private int port;
    @Value("${custom.netty.pre-sale.websocket.boss-thread-count:1}")
    private int bossThreadCount;
    @Value("${custom.netty.pre-sale.websocket.worker-thread-count:0}")
    private int workerThreadCount;
    @Value("${custom.netty.pre-sale.websocket.idle-timeout:600}")
    private int idleTimeout;

    // 售前WebSocket基础路径（独立，与售后区分）
    private static final String PRE_SALE_WS_BASE_PATH = "/pre-sale/websocket";
    // 通道属性-捕获前端URI
    public static final AttributeKey<String> CLIENT_WEBSOCKET_URI = AttributeKey.valueOf("PRE_SALE_CLIENT_WEBSOCKET_URI");

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture serverFuture;

    /**
     * 项目启动时初始化Netty服务（@PostConstruct）
     */
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
                                log.info("【售前-通道初始化】通道ID：{}", channelId);

                                // 1. HTTP基础处理器
                                pipeline.addLast(new HttpServerCodec());
                                pipeline.addLast(new ChunkedWriteHandler());
                                pipeline.addLast(new HttpObjectAggregator(64 * 1024 * 1024));

                                // 2. 捕获前端WebSocket URI（协议升级前）
                                pipeline.addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        if (msg instanceof io.netty.handler.codec.http.HttpRequest) {
                                            io.netty.handler.codec.http.HttpRequest request = (io.netty.handler.codec.http.HttpRequest) msg;
                                            if (request.method().equals(io.netty.handler.codec.http.HttpMethod.GET)) {
                                                String uri = request.uri();
                                                ctx.channel().attr(CLIENT_WEBSOCKET_URI).set(uri);
                                                log.info("【售前-URI捕获】通道ID：{}，URI：{}", channelId, uri);
                                                super.channelRead(ctx, msg);
                                                ctx.pipeline().remove(this);
                                                return;
                                            }
                                        }
                                        super.channelRead(ctx, msg);
                                    }
                                });

                                // 3. 心跳检测
                                pipeline.addLast(new IdleStateHandler(idleTimeout, 0, 0, TimeUnit.SECONDS));

                                // 4. WebSocket协议升级（售前独立路径）
                                WebSocketServerProtocolHandler wsProtocolHandler = new WebSocketServerProtocolHandler(
                                        PRE_SALE_WS_BASE_PATH,
                                        null,
                                        true,
                                        1024 * 1024,
                                        false,
                                        true
                                );
                                pipeline.addLast("preSaleWebSocketProtocolHandler", wsProtocolHandler);

                                // 5. 握手完成事件监听（售前独立业务绑定）
                                pipeline.addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                        if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                                            log.info("【售前-协议升级】通道ID：{}，WebSocket握手成功", channelId);
                                            Channel channel = ctx.channel();
                                            String uri = channel.attr(CLIENT_WEBSOCKET_URI).get();

                                            // 解析售前ReceiverId（用户/客服ID）
                                            if (uri != null && uri.startsWith(PRE_SALE_WS_BASE_PATH + "/")) {
                                                String receiverId = uri.substring((PRE_SALE_WS_BASE_PATH + "/").length());
                                                receiverId = receiverId.contains("?") ? receiverId.split("\\?")[0] : receiverId;

                                                // 校验ReceiverId格式（售前专属：LYQY_USER_/LYQY_CS_）
                                                if (!receiverId.startsWith("LYQY_USER_") && !receiverId.startsWith("LYQY_CS_")) {
                                                    log.error("【售前-会话注册失败】通道ID：{}，ReceiverId格式错误：{}", channelId, receiverId);
                                                    ctx.channel().close();
                                                    return;
                                                }

                                                // 解析售前会话ID（PRE_SESSION_xxx）
                                                String preSaleSessionId = parsePreSaleSessionIdFromUri(uri);
                                                if (preSaleSessionId == null || !preSaleSessionId.startsWith("PRE_SESSION_")) {
                                                    log.error("【售前-会话注册失败】通道ID：{}，未传入合法售前会话ID", channelId);
                                                    ctx.channel().close();
                                                    return;
                                                }

                                                // 区分发送者类型
                                                String senderType = receiverId.startsWith("LYQY_USER_")
                                                        ? PreSaleChatMessageVO.SENDER_TYPE_USER
                                                        : PreSaleChatMessageVO.SENDER_TYPE_CS;

                                                // 绑定售前专属常量到通道（核心隔离）
                                                channel.attr(NettyConstant.PRE_SALE_RECEIVER_ID_KEY).set(receiverId);
                                                channel.attr(NettyConstant.PRE_SALE_SESSION_ID_KEY).set(preSaleSessionId);
                                                channel.attr(NettyConstant.PRE_SALE_SENDER_TYPE_KEY).set(senderType);
                                                // 注册到售前独立通道映射
                                                PreSaleNettyWebSocketServerHandler.PRE_SALE_RECEIVER_CHANNEL_MAP.put(receiverId, channel);

                                                log.info("【售前-会话注册成功】通道ID：{}，ReceiverId：{}，类型：{}，会话ID：{}",
                                                        channelId, receiverId, senderType, preSaleSessionId);

                                                // 推送售前未读消息（优化：处理ReceiverId前缀，和售后对齐）
                                                pushUnreadPreSaleMessage(channel, receiverId, preSaleSessionId);

                                                // 清理HTTP处理器
                                                cleanHttpHandlers(ctx, channelId);
                                            } else {
                                                log.error("【售前-会话注册失败】通道ID：{}，URI格式错误：{}", channelId, uri);
                                            }
                                            ctx.pipeline().remove(this);
                                            return;
                                        }
                                        super.userEventTriggered(ctx, evt);
                                    }
                                });

                                // 6. 售前专属编解码器
                                pipeline.addLast(new PreSaleWebSocketMsgCodec());

                                // 7. 售前专属业务处理器
                                pipeline.addLast(new PreSaleNettyWebSocketServerHandler());

                                log.info("【售前-通道初始化完成】通道ID：{}，处理器链路：{}", channelId, pipeline.names());
                            }
                        });

                // 绑定售前独立端口8089
                serverFuture = bootstrap.bind(port).sync();
                log.info("=====================================");
                log.info("售前Netty WebSocket服务启动成功");
                log.info("端口：{}", port);
                log.info("WebSocket路径：{}", PRE_SALE_WS_BASE_PATH);
                log.info("=====================================");
                serverFuture.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("售前Netty服务启动失败：{}", e.getMessage(), e);
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
                log.info("售前Netty EventLoopGroup 已优雅关闭");
            }
        }, "PreSale-Netty-WebSocket-Server-Thread").start();
    }

    /**
     * 解析URI中的售前会话ID（preSaleSessionId）
     */
    private String parsePreSaleSessionIdFromUri(String uri) {
        if (uri == null || !uri.contains("?preSaleSessionId=")) {
            return null;
        }
        String paramPart = uri.substring(uri.indexOf("?") + 1);
        String[] paramPairs = paramPart.split("&");
        for (String pair : paramPairs) {
            if (pair.startsWith("preSaleSessionId=")) {
                return pair.substring("preSaleSessionId=".length()).trim();
            }
        }
        return null;
    }

    /**
     * 推送售前未读消息（优化：处理ReceiverId前缀，和售后对齐）
     */
    private void pushUnreadPreSaleMessage(Channel channel, String receiverId, String preSaleSessionId) {
        try {
            PreSaleChatMessageService preSaleChatMessageService = SpringContextUtil.getBean(PreSaleChatMessageService.class);

            // 注意：如果Service层已处理前缀，这里直接传原始receiverId即可（无需拼接R_FIXED_0000_）
            // 若权限校验必须用前缀，需确保Service层能正确解析
            Result<List<PreSaleChatMessagePO>> unreadResult = preSaleChatMessageService.listUnreadBySessionAndReceiver(preSaleSessionId, receiverId);

            if (unreadResult != null && unreadResult.isSuccess() && !CollectionUtils.isEmpty(unreadResult.getData())) {
                List<PreSaleChatMessagePO> unreadList = unreadResult.getData();
                log.info("【售前-未读消息推送】ReceiverId：{}，找到{}条未读消息", receiverId, unreadList.size());

                // SQL已筛选is_read=0，无需再判断
                for (PreSaleChatMessagePO msg : unreadList) {
                    channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(msg)));
                }

                log.info("【售前-未读消息推送完成】ReceiverId：{}，推送{}条", receiverId, unreadList.size());
            } else {
                log.info("【售前-未读消息查询】ReceiverId：{}，无未读消息", receiverId);
            }
        } catch (Exception e) {
            log.error("【售前-未读消息推送异常】ReceiverId：{}，异常：{}", receiverId, e.getMessage(), e);
        }
    }

    /**
     * 清理HTTP处理器
     */
    private void cleanHttpHandlers(ChannelHandlerContext ctx, String channelId) {
        try {
            if (ctx.pipeline().get(HttpServerCodec.class) != null) ctx.pipeline().remove(HttpServerCodec.class);
            if (ctx.pipeline().get(HttpObjectAggregator.class) != null) ctx.pipeline().remove(HttpObjectAggregator.class);
            if (ctx.pipeline().get(ChunkedWriteHandler.class) != null) ctx.pipeline().remove(ChunkedWriteHandler.class);
        } catch (Exception e) {
            log.warn("【售前-处理器清理异常】通道ID：{}，异常：{}", channelId, e.getMessage());
        }
    }

    /**
     * 项目关闭时优雅关闭Netty（@PreDestroy）
     */
    @PreDestroy
    public void stop() {
        if (serverFuture != null) {
            serverFuture.channel().close().syncUninterruptibly();
        }
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        log.info("售前Netty WebSocket服务已关闭");
    }
}