package com.yqrb.server;

import com.yqrb.handler.CustomWebSocketBusinessHandler;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * Netty WebSocket 服务启动类（纯内存运行，无数据库依赖，独立端口）
 */
@Slf4j
@Component
public class NettyWebSocketServer {
    /** WebSocket 服务端口（配置在 application.yml） */
    @Value("${netty.websocket.port:8081}")
    private int port;

    @Autowired
    private CustomWebSocketBusinessHandler customWebSocketBusinessHandler;

    /** 主线程组（接受客户端连接） */
    private EventLoopGroup bossGroup;

    /** 工作线程组（处理客户端读写事件） */
    private EventLoopGroup workerGroup;

    // ==================== 启动 Netty 服务（SpringBoot 启动后异步执行，不阻塞主线程） ====================
    @PostConstruct
    public void start() {
        new Thread(() -> {
            // 初始化线程组
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();

            try {
                // 1. 创建 ServerBootstrap 实例
                ServerBootstrap serverBootstrap = new ServerBootstrap();
                serverBootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class) // 指定 NIO 通道类型
                        .option(ChannelOption.SO_BACKLOG, 1024) // 连接队列大小
                        .childOption(ChannelOption.SO_KEEPALIVE, true) // 开启长连接
                        .childOption(ChannelOption.TCP_NODELAY, true) // 关闭Nagle算法，提升实时性
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline()
                                        // 1. 心跳检测处理器（防止长连接被断开）：30秒读空闲、30秒写空闲、60秒读写空闲
                                        .addLast(new IdleStateHandler(30, 30, 60, TimeUnit.SECONDS))
                                        // 2. HTTP 编解码器
                                        .addLast(new HttpServerCodec())
                                        // 3. 大数据流处理器（支持大消息传输）
                                        .addLast(new ChunkedWriteHandler())
                                        // 4. HTTP 消息聚合器（将 HttpRequest + HttpContent 聚合为 FullHttpRequest，最大10M）
                                        .addLast(new HttpObjectAggregator(1024 * 1024 * 10))
                                        // 5. WebSocket 协议处理器（指定 WebSocket 路径为 /ws，支持跨域）
                                        .addLast(new WebSocketServerProtocolHandler("/ws", null, true, 1024 * 1024 * 10))
                                        // 6. 自定义业务处理器（处理文本消息，纯内存操作）
                                        .addLast(customWebSocketBusinessHandler);
                            }
                        });

                // 2. 绑定端口，启动服务
                ChannelFuture future = serverBootstrap.bind(port).sync();
                log.info("Netty WebSocket 服务启动成功（纯内存版），端口：{}", port);

                // 3. 等待服务关闭（阻塞）
                future.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                log.error("Netty WebSocket 服务启动/运行异常", e);
                Thread.currentThread().interrupt();
            } finally {
                // 关闭线程组
                if (bossGroup != null) {
                    bossGroup.shutdownGracefully();
                }
                if (workerGroup != null) {
                    workerGroup.shutdownGracefully();
                }
            }
        }, "netty-websocket-server-thread").start();
    }

    // ==================== 关闭 Netty 服务（SpringBoot 关闭前执行） ====================
    @PreDestroy
    public void stop() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("Netty WebSocket 服务已关闭（纯内存版）");
    }
}