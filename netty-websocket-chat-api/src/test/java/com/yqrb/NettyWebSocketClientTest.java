package com.yqrb;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
// 仅保留核心WebSocket导入（无WebSocketCloseFrame）
import io.netty.handler.codec.http.websocketx.*;

import java.net.URI;
import java.util.Scanner;

/**
 * 适配融媒体登报系统的Netty WebSocket客户端
 * 包名适配：com.yqrb.netty
 * 配置适配：服务端端口8081、路径/newspaper/websocket/{sessionId}
 * 依赖移除：彻底移除WebSocketCloseFrame相关引用
 * 优化：支持多行JSON输入（空行触发发送，exit断开连接）
 */
public class NettyWebSocketClientTest {

    // 适配服务端配置：端口改为8081，路径匹配/newspaper/websocket/{sessionId}
    private static final String WS_HOST = "127.0.0.1";
    private static final int WS_PORT = 8081; // 匹配服务端配置的8081端口
    private static final String SESSION_ID = "SESSION_cf22da7ebff04caa9b40f61a41d0f465";
    private static final String WS_URL = String.format("ws://%s:%d/newspaper/websocket/%s", WS_HOST, WS_PORT, SESSION_ID);

    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            // HTTP编解码（WebSocket基于HTTP握手）
                            pipeline.addLast(new HttpClientCodec());
                            // 聚合HTTP消息（匹配服务端1MB限制）
                            pipeline.addLast(new HttpObjectAggregator(1024 * 1024));
                            // WebSocket客户端处理器（适配服务端路径）
                            pipeline.addLast(new NettyWebSocketClientHandler(WS_URL));
                        }
                    });

            // 连接服务端（匹配服务端端口和路径）
            URI uri = new URI(WS_URL);
            Channel channel = bootstrap.connect(uri.getHost(), uri.getPort()).sync().channel();
            System.out.println("=== 已连接融媒体登报WebSocket服务端（端口：8081） ===");
            System.out.println("示例消息格式：{\"receiverId\":\"xxx\",\"userId\":\"xxx\",\"msgContent\":\"xxx\",\"msgType\":\"TEXT\",\"sessionId\":\"" + SESSION_ID + "\"}");
            System.out.println("输入多行JSON消息（空行触发发送，输入 exit 断开连接）：");

            // 优化：控制台输入多行JSON消息（空行发送，exit退出）
            Scanner scanner = new Scanner(System.in);
            StringBuilder msgBuilder = new StringBuilder();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                // 1. 输入exit：断开连接
                if ("exit".equalsIgnoreCase(line)) {
                    channel.close().sync();
                    break;
                }
                // 2. 空行：发送已输入的完整JSON
                if (line.isEmpty()) {
                    String fullMsg = msgBuilder.toString().trim();
                    if (!fullMsg.isEmpty()) {
                        TextWebSocketFrame textFrame = new TextWebSocketFrame(fullMsg);
                        channel.writeAndFlush(textFrame);
                        System.out.println("✅ 完整消息已发送：\n" + fullMsg);
                        msgBuilder.setLength(0); // 清空缓冲区
                    }
                    continue;
                }
                // 3. 非空行：拼接JSON行
                msgBuilder.append(line);
            }

            // 等待通道关闭
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    /**
     * 适配业务的WebSocket客户端处理器
     * 移除所有WebSocketCloseFrame相关逻辑，匹配服务端通信规则
     */
    static class NettyWebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

        private final WebSocketClientHandshaker handshaker;
        private ChannelPromise handshakeFuture;

        public NettyWebSocketClientHandler(String wsUrl) {
            this.handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                    URI.create(wsUrl),
                    WebSocketVersion.V13,
                    null,
                    true,
                    new DefaultHttpHeaders()
            );
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            handshakeFuture = ctx.newPromise();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            System.out.println("🔌 开始WebSocket握手（路径：" + handshaker.uri() + "）");
            handshaker.handshake(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            System.out.println("❌ 与服务端断开连接");
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            Channel ch = ctx.channel();
            // 处理握手响应
            if (!handshaker.isHandshakeComplete()) {
                handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                System.out.println("✅ WebSocket握手成功（业务路径：" + handshaker.uri() + "）");
                handshakeFuture.setSuccess();
                return;
            }

            // 处理服务端推送的业务消息
            if (msg instanceof FullHttpResponse) {
                FullHttpResponse response = (FullHttpResponse) msg;
                throw new IllegalStateException("❌ WebSocket握手失败：" + response.status());
            }

            WebSocketFrame frame = (WebSocketFrame) msg;
            // 仅处理文本消息（移除CloseFrame相关判断，依赖channelInactive处理断开）
            if (frame instanceof TextWebSocketFrame) {
                String respMsg = ((TextWebSocketFrame) frame).text();
                System.out.println("\n📩 收到服务端业务回复：\n" + respMsg);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            System.err.println("❌ 客户端异常：");
            cause.printStackTrace();
            if (!handshakeFuture.isDone()) {
                handshakeFuture.setFailure(cause);
            }
            ctx.close();
        }

        public ChannelFuture handshakeFuture() {
            return handshakeFuture;
        }
    }
}