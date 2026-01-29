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
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.codec.http.websocketx.*;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * 融媒体登报系统WebSocket接收端测试程序（模拟客服端/被动接收方）
 * 核心：专注接收消息、心跳保活、断线重连、解析登报业务消息
 * 配置：匹配服务端8081端口、/newspaper/websocket/{sessionId}路径
 * 移除：WebSocketCloseFrame相关引用
 * 修复：兼容低版本Java（替换String.repeat()）、修正构造器参数类型不匹配
 */
public class NettyWebSocketReceiverTest {
    // 接收端专属SessionId（区分发送端，模拟不同角色）
    private static final String RECEIVER_SESSION_ID = "SESSION_8899aabbccddeeff0011223344556677";
    private static final String WS_HOST = "127.0.0.1";
    private static final int WS_PORT = 8081;
    private static final String WS_URL = String.format("ws://%s:%d/newspaper/websocket/%s", WS_HOST, WS_PORT, RECEIVER_SESSION_ID);

    private EventLoopGroup group;
    private Bootstrap bootstrap;
    private Channel channel;

    public static void main(String[] args) throws Exception {
        NettyWebSocketReceiverTest receiver = new NettyWebSocketReceiverTest();
        // 初始化并连接服务端
        receiver.init();
        // 保持程序运行（接收端长期在线）
        receiver.keepAlive();
    }

    /**
     * 初始化客户端配置（含心跳、重连）
     */
    public void init() throws Exception {
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new LoggingHandler(LogLevel.INFO))
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        // HTTP编解码（WebSocket握手基础）
                        pipeline.addLast(new HttpClientCodec());
                        // 聚合HTTP消息（匹配服务端1MB限制）
                        pipeline.addLast(new HttpObjectAggregator(1024 * 1024));
                        // 心跳检测：30秒无读写则触发Idle事件（保活+断线检测）
                        pipeline.addLast(new IdleStateHandler(0, 0, 30, TimeUnit.SECONDS));
                        // 修复点1：使用 NettyWebSocketReceiverTest.this 传入外层类实例（解决类型不匹配）
                        pipeline.addLast(new NettyWebSocketReceiverHandler(WS_URL, NettyWebSocketReceiverTest.this));
                    }
                });
        // 首次连接服务端
        connect();
    }

    /**
     * 连接/重连服务端
     */
    public void connect() throws Exception {
        URI uri = new URI(WS_URL);
        ChannelFuture future = bootstrap.connect(uri.getHost(), uri.getPort());
        future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                channel = f.channel();
                System.out.println("=== 接收端已连接融媒体登报WebSocket服务端 ===");
                System.out.println("接收端SessionId：" + RECEIVER_SESSION_ID);
                System.out.println("等待接收消息（登报通知/用户消息）...\n");
            } else {
                System.err.println("❌ 接收端连接失败，5秒后重试...");
                f.channel().eventLoop().schedule(() -> {
                    try {
                        connect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, 5, TimeUnit.SECONDS);
            }
        });
        // 等待连接关闭（触发重连）
        future.channel().closeFuture().addListener((ChannelFutureListener) f -> {
            try {
                connect(); // 断线自动重连
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 保持程序存活（接收端长期运行）
     */
    public void keepAlive() throws InterruptedException {
        synchronized (this) {
            this.wait();
        }
    }

    /**
     * 接收端专属处理器（解析业务消息、心跳、重连）
     */
    static class NettyWebSocketReceiverHandler extends SimpleChannelInboundHandler<Object> {
        private final WebSocketClientHandshaker handshaker;
        private ChannelPromise handshakeFuture;
        private final NettyWebSocketReceiverTest receiver; // 关联外层重连逻辑

        public NettyWebSocketReceiverHandler(String wsUrl, NettyWebSocketReceiverTest receiver) {
            this.handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                    URI.create(wsUrl),
                    WebSocketVersion.V13,
                    null,
                    true,
                    new DefaultHttpHeaders()
            );
            this.receiver = receiver;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            handshakeFuture = ctx.newPromise();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            System.out.println("🔌 接收端开始WebSocket握手...");
            handshaker.handshake(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            System.err.println("❌ 接收端与服务端断开连接，准备重连...");
        }

        /**
         * 核心：处理服务端推送的所有消息（登报通知/用户聊天消息）
         */
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            Channel ch = ctx.channel();
            // 完成握手
            if (!handshaker.isHandshakeComplete()) {
                handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                System.out.println("✅ 接收端WebSocket握手成功");
                handshakeFuture.setSuccess();
                return;
            }

            // 处理异常HTTP响应
            if (msg instanceof FullHttpResponse) {
                FullHttpResponse response = (FullHttpResponse) msg;
                throw new IllegalStateException("❌ 接收端握手失败：" + response.status());
            }

            // 解析业务消息（仅处理文本帧，登报消息为JSON格式）
            WebSocketFrame frame = (WebSocketFrame) msg;
            if (frame instanceof TextWebSocketFrame) {
                String respMsg = ((TextWebSocketFrame) frame).text();
                System.out.println("📩 接收端收到业务消息：");
                // 格式化输出JSON（便于查看登报订单/消息内容）
                System.out.println(formatJson(respMsg) + "\n");
            }
        }

        /**
         * 心跳处理：30秒无交互则发送Ping帧保活
         */
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof io.netty.handler.timeout.IdleStateEvent) {
                // 发送Ping帧，服务端回复Pong帧（保活）
                ctx.channel().writeAndFlush(new PingWebSocketFrame());
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            System.err.println("❌ 接收端异常：");
            cause.printStackTrace();
            if (!handshakeFuture.isDone()) {
                handshakeFuture.setFailure(cause);
            }
            ctx.close(); // 关闭连接触发重连
        }

        /**
         * 修复点2：自定义JSON格式化（替换String.repeat()，兼容Java 8及以下）
         */
        private String formatJson(String json) {
            if (json == null || json.isEmpty()) return json;
            StringBuilder sb = new StringBuilder();
            int level = 0;
            for (char c : json.toCharArray()) {
                if (c == '{' || c == '[') {
                    sb.append(c).append("\n");
                    level++;
                    // 调用自定义工具方法，替代 "\t".repeat(level)
                    sb.append(repeatTab(level));
                } else if (c == '}' || c == ']') {
                    sb.append("\n");
                    level--;
                    // 调用自定义工具方法，替代 "\t".repeat(level)
                    sb.append(repeatTab(level));
                    sb.append(c);
                } else if (c == ',') {
                    sb.append(c).append("\n");
                    // 调用自定义工具方法，替代 "\t".repeat(level)
                    sb.append(repeatTab(level));
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        /**
         * 自定义制表符重复方法（兼容低版本Java，替代String.repeat()）
         * @param times 重复次数
         * @return 重复后的制表符字符串
         */
        private String repeatTab(int times) {
            if (times <= 0) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < times; i++) {
                sb.append("\t");
            }
            return sb.toString();
        }

        public ChannelFuture handshakeFuture() {
            return handshakeFuture;
        }
    }
}