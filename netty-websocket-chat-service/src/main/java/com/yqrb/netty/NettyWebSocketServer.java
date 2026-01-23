package com.yqrb.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
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

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @PostConstruct
    public void start() {
        // 核心改动1：新建独立线程运行Netty，不阻塞Spring主线程
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
                                        .addLast(new HttpServerCodec())
                                        .addLast(new ChunkedWriteHandler())
                                        .addLast(new HttpObjectAggregator(1024 * 1024))
                                        .addLast(new IdleStateHandler(idleTimeout, 0, 0, TimeUnit.SECONDS))
                                        .addLast(new WebSocketServerProtocolHandler("/ws"))
                                        .addLast(new NettyWebSocketServerHandler())
                                        .addLast(new WebSocketMsgCodec());
                            }
                        });

                // 核心改动2：去掉sync()，改用addListener异步监听启动结果
                ChannelFuture future = bootstrap.bind(port).addListener(f -> {
                    if (f.isSuccess()) {
                        System.out.println("Netty WebSocket服务启动成功，监听端口：" + port);
                    } else {
                        System.err.println("Netty WebSocket服务启动失败：" + f.cause());
                        // 启动失败时优雅关闭线程组
                        bossGroup.shutdownGracefully();
                        workerGroup.shutdownGracefully();
                    }
                });

                // 核心改动3：去掉sync()，改用addListener异步监听关闭事件
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
        }, "Netty-WebSocket-Server-Thread").start(); // 给线程命名，便于排查问题
    }

    @PreDestroy
    public void stop() {
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        System.out.println("Netty WebSocket服务（通过@PreDestroy）已关闭");
    }
}