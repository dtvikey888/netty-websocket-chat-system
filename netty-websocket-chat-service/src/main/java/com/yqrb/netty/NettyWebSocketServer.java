package com.yqrb.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
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

    @Value("${custom.netty.websocket.port}")
    private int port;

    @Value("${custom.netty.websocket.boss-thread-count}")
    private int bossThreadCount;

    @Value("${custom.netty.websocket.worker-thread-count}")
    private int workerThreadCount;

    @Value("${custom.netty.websocket.idle-timeout}")
    private int idleTimeout;

    // 自定义AttributeKey，存储客户端WebSocket请求URI（替代WEBSOCKET_URI）
    public static final AttributeKey<String> CLIENT_WEBSOCKET_URI = AttributeKey.valueOf("CLIENT_WEBSOCKET_URI");

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @PostConstruct
    public void start() {
        new Thread(() -> {
            bossGroup = new NioEventLoopGroup(bossThreadCount);
            workerGroup = new NioEventLoopGroup(workerThreadCount);

            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, 1024)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline()
                                        // 1. HTTP编解码
                                        .addLast(new HttpServerCodec())
                                        // 2. 大文件处理
                                        .addLast(new ChunkedWriteHandler())
                                        // 3. HTTP消息聚合
                                        .addLast(new HttpObjectAggregator(1024 * 1024))
                                        // ========== 核心新增：捕获客户端URI ==========
                                        .addLast(new ChannelInboundHandlerAdapter() {
                                            @Override
                                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                                // 仅处理首次HTTP请求（WebSocket升级请求）
                                                if (msg instanceof HttpRequest) {
                                                    HttpRequest request = (HttpRequest) msg;
                                                    String uri = request.uri();
                                                    // 将URI存入Channel自定义属性
                                                    ctx.channel().attr(CLIENT_WEBSOCKET_URI).set(uri);
                                                    System.out.println("捕获客户端WebSocket请求URI：" + uri);
                                                    // 处理完后移除当前Handler（避免重复处理）
                                                    ctx.pipeline().remove(this);
                                                }
                                                // 传递给下一个Handler
                                                super.channelRead(ctx, msg);
                                            }
                                        })
                                        // 4. WebSocket协议升级（固定前缀）
                                        .addLast(new WebSocketServerProtocolHandler("/newspaper/websocket", null, true, 1024 * 1024))
                                        // 5. 心跳检测
                                        .addLast(new IdleStateHandler(idleTimeout, 0, 0, TimeUnit.SECONDS))
                                        // 6. 自定义编解码器
                                        .addLast(new WebSocketMsgCodec())
                                        // 7. 业务Handler
                                        .addLast(new NettyWebSocketServerHandler());
                            }
                        });

                ChannelFuture future = bootstrap.bind(port).addListener(f -> {
                    if (f.isSuccess()) {
                        System.out.println("Netty WebSocket服务启动成功，监听端口：" + port);
                    } else {
                        System.err.println("Netty WebSocket服务启动失败：" + f.cause());
                        bossGroup.shutdownGracefully();
                        workerGroup.shutdownGracefully();
                    }
                });

                future.channel().closeFuture().addListener(f -> {
                    System.out.println("Netty WebSocket服务开始关闭...");
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                    System.out.println("Netty WebSocket服务已关闭");
                });

            } catch (Exception e) {
                e.printStackTrace();
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }, "Netty-WebSocket-Server-Thread").start();
    }

    @PreDestroy
    public void stop() {
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        System.out.println("Netty WebSocket服务（通过@PreDestroy）已关闭");
    }
}