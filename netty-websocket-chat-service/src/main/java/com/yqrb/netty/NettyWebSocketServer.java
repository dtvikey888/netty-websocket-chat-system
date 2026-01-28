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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@Component
public class NettyWebSocketServer {
    // ===== 适配你的配置：端口8081，WebSocket路径/newspaper/websocket =====
    @Value("${custom.netty.websocket.port:8081}")
    private int port;
    @Value("${custom.netty.websocket.boss-thread-count:1}")
    private int bossThreadCount;
    @Value("${custom.netty.websocket.worker-thread-count:0}") // 0=默认CPU核心数*2
    private int workerThreadCount;
    @Value("${custom.netty.websocket.idle-timeout:30}") // 30秒心跳超时
    private int idleTimeout;

    // 自定义AttributeKey：存储客户端URI（替代不稳定的WEBSOCKET_URI）
    public static final AttributeKey<String> CLIENT_WEBSOCKET_URI = AttributeKey.valueOf("CLIENT_WEBSOCKET_URI");

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture serverFuture;

    // ===== Spring环境下异步启动（避免阻塞Spring初始化）=====
    @PostConstruct
    public void start() {
        new Thread(() -> {
            // 初始化线程组（适配你的配置）
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

                                // ===== 1. HTTP基础处理器（WebSocket基于HTTP握手）=====
                                pipeline.addLast(new HttpServerCodec()); // HTTP编解码
                                pipeline.addLast(new ChunkedWriteHandler()); // 大文件支持
                                pipeline.addLast(new HttpObjectAggregator(1024 * 1024)); // HTTP消息聚合

                                // ===== 2. 核心适配：捕获客户端URI（解决WEBSOCKET_URI解析问题）=====
                                pipeline.addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        if (msg instanceof io.netty.handler.codec.http.HttpRequest) {
                                            io.netty.handler.codec.http.HttpRequest request = (io.netty.handler.codec.http.HttpRequest) msg;
                                            String uri = request.uri();
                                            // 存储URI到Channel属性（供Handler解析sessionId）
                                            ctx.channel().attr(CLIENT_WEBSOCKET_URI).set(uri);
                                            System.out.println("捕获客户端URI：" + uri);
                                            // 处理完移除当前Handler，避免重复处理
                                            ctx.pipeline().remove(this);
                                        }
                                        super.channelRead(ctx, msg);
                                    }
                                });

                                // ===== 3. WebSocket协议升级（适配你的路径/newspaper/websocket）=====
                                // 注意：路径末尾不加/，客户端URI是/newspaper/websocket/SESSION_xxx
                                pipeline.addLast(new WebSocketServerProtocolHandler("/newspaper/websocket", null, true, 1024 * 1024));

                                // ===== 4. 心跳检测（适配你的idleTimeout配置）=====
                                pipeline.addLast(new IdleStateHandler(idleTimeout, 0, 0, TimeUnit.SECONDS));

                                // ===== 5. 你的自定义编解码器（保持原有逻辑）=====
                                pipeline.addLast(new WebSocketMsgCodec());

                                // ===== 6. 你的业务Handler（保持原有逻辑）=====
                                pipeline.addLast(new NettyWebSocketServerHandler());
                            }
                        });

                // 绑定端口（适配你的8081端口）
                serverFuture = bootstrap.bind(port).sync();
                System.out.println("Netty WebSocket服务启动成功：端口=" + port + "，路径=/newspaper/websocket");

                // 阻塞等待服务关闭
                serverFuture.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Netty服务启动失败：" + e.getMessage());
            } finally {
                // 优雅关闭线程组
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }, "Netty-WebSocket-Server-Thread").start();
    }

    // ===== Spring销毁时优雅关闭Netty =====
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
        System.out.println("Netty WebSocket服务已关闭");
    }
}