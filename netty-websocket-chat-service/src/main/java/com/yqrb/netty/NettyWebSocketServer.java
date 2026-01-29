package com.yqrb.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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

    public static final AttributeKey<String> CLIENT_WEBSOCKET_URI = AttributeKey.valueOf("CLIENT_WEBSOCKET_URI");

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture serverFuture;

    // 移除全局单例 Handler！！！

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
                                pipeline.addLast(new HttpObjectAggregator(64*1024 * 1024)); // HTTP消息聚合

                                // ===== 2. 捕获客户端URI（必须在协议升级前）=====
                                // ===== 2. 捕获客户端URI（必须在协议升级前）=====
                                pipeline.addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        if (msg instanceof io.netty.handler.codec.http.HttpRequest) {
                                            io.netty.handler.codec.http.HttpRequest request = (io.netty.handler.codec.http.HttpRequest) msg;
                                            // 新增：仅处理GET请求（WebSocket握手是GET请求）
                                            if (request.method().equals(io.netty.handler.codec.http.HttpMethod.GET)) {
                                                String uri = request.uri();
                                                ctx.channel().attr(CLIENT_WEBSOCKET_URI).set(uri);
                                                log.info("【URI捕获】通道ID：{}，URI：{}", channelId, uri);

                                                // 修复：先传递消息，再移除自身Handler（确保请求不被阻断）
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

                                // ===== 3. 心跳检测（★ 修复：移动到WebSocket协议处理器之前 ★）=====
                                pipeline.addLast(new IdleStateHandler(idleTimeout, 0, 0, TimeUnit.SECONDS));

                                // ===== 4. WebSocket协议升级（核心！必须在HTTP处理器后、编解码器前）=====
                                pipeline.addLast(new WebSocketServerProtocolHandler("/newspaper/websocket", null, true, 1024 * 1024) {
                                    @Override
                                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                        if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                                            log.info("【协议升级】通道ID：{}，WebSocket握手成功", channelId);

                                            // ===== 新增：握手成功后，解析客服ID并注册到RECEIVER_CHANNEL_MAP =====
                                            Channel channel = ctx.channel();
                                            // 1. 获取之前捕获的URI
                                            String uri = channel.attr(NettyWebSocketServer.CLIENT_WEBSOCKET_URI).get();
                                            if (uri != null && uri.startsWith("/newspaper/websocket/")) {
                                                // 2. 解析出客服ID（sessionId）
                                                String csReceiverId = uri.substring("/newspaper/websocket/".length());
                                                // 3. 存入通道的SESSION_ID_KEY（保持和原有逻辑一致）
                                                channel.attr(NettyWebSocketServerHandler.SESSION_ID_KEY).set(csReceiverId);
                                                // 4. 注册到客服通道映射表（★ 确保ID无前缀，和后续查询一致 ★）
                                                NettyWebSocketServerHandler.RECEIVER_CHANNEL_MAP.put(csReceiverId, channel);
                                                log.info("【会话注册成功】通道ID：{}，客服ID：{}，已加入在线映射表", channelId, csReceiverId);
                                            } else {
                                                log.error("【会话注册失败】通道ID：{}，URI格式错误：{}", channelId, uri);
                                            }

                                            // 握手成功后移除HTTP处理器（优化性能，保留原有逻辑）
                                            ctx.pipeline().remove(HttpServerCodec.class);
                                            ctx.pipeline().remove(HttpObjectAggregator.class);
                                            ctx.pipeline().remove(ChunkedWriteHandler.class);
                                        }
                                        super.userEventTriggered(ctx, evt);
                                    }
                                });

                                // ===== 5. 自定义编解码器（必须在协议升级后）=====
                                pipeline.addLast(new WebSocketMsgCodec());

                                // ===== 6. 业务处理器（最后加）=====
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