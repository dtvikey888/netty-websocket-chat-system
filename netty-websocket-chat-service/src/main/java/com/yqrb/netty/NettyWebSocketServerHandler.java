package com.yqrb.netty;

import com.alibaba.fastjson.JSON;
import com.yqrb.pojo.vo.WebSocketMsgVO;
import com.yqrb.service.ReceiverIdService;
import com.yqrb.util.DateUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * Netty WebSocket消息处理器（处理连接、断开、消息转发）
 */
@Component
public class NettyWebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketMsgVO> {
    // 保留你的原有属性
    private static final ChannelGroup ONLINE_CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final Map<String, Channel> RECEIVER_CHANNEL_MAP = new ConcurrentHashMap<>();
    private static final AttributeKey<String> SESSION_ID_KEY = AttributeKey.valueOf("SESSION_ID");
    private static ReceiverIdService receiverIdService;

    @Autowired
    private ReceiverIdService receiverIdServiceAutowire;

    @PostConstruct
    public void init() {
        NettyWebSocketServerHandler.receiverIdService = this.receiverIdServiceAutowire;
        System.out.println("ReceiverIdService注入完成：" + (receiverIdService != null));
    }

    // ===== 核心适配：解析sessionId（改用自定义的CLIENT_WEBSOCKET_URI）=====
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        ONLINE_CHANNELS.add(channel);

        // 1. 从自定义AttributeKey获取URI（替代WEBSOCKET_URI）
        String uri = channel.attr(NettyWebSocketServer.CLIENT_WEBSOCKET_URI).get();
        if (uri != null && uri.startsWith("/newspaper/websocket/")) {
            // 解析sessionId：截取/newspaper/websocket/后的部分
            String sessionId = uri.substring("/newspaper/websocket/".length());
            channel.attr(SESSION_ID_KEY).set(sessionId);
            System.out.println("解析到sessionId：" + sessionId + "，通道ID：" + channel.id().asShortText());
        }

        // 2. 保留你的原有日志
        System.out.println("新客户端连接：" + channel.id().asShortText() + "，在线人数：" + ONLINE_CHANNELS.size());
    }

    // ===== 保留你的原有业务逻辑（仅确认方法参数类型匹配）=====
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 你的原有代码（无需修改）
        Channel channel = ctx.channel();
        ONLINE_CHANNELS.remove(channel);

        RECEIVER_CHANNEL_MAP.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(channel)) {
                String receiverId = entry.getKey();
                if (receiverIdService != null) {
                    receiverIdService.markOffline(receiverId);
                }
                System.out.println("客户端离线：" + receiverId + "，通道：" + channel.id().asShortText());
                return true;
            }
            return false;
        });

        String sessionId = channel.attr(SESSION_ID_KEY).get();
        System.out.println("客户端断开连接：通道=" + channel.id().asShortText()
                + "，sessionId=" + (sessionId == null ? "未知" : sessionId)
                + "，当前在线人数：" + ONLINE_CHANNELS.size());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 你的原有代码（无需修改）
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleEvent = (IdleStateEvent) evt;
            if (idleEvent.state() == IdleState.READER_IDLE) {
                Channel channel = ctx.channel();
                String sessionId = channel.attr(SESSION_ID_KEY).get();
                System.out.println("客户端空闲超时，断开连接：通道=" + channel.id().asShortText()
                        + "，sessionId=" + (sessionId == null ? "未知" : sessionId));
                channel.close();
                return;
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketMsgVO webSocketMsg) throws Exception {
        // 你的原有代码（无需修改，仅补充一行强制日志确认消息到达）
        System.err.println("===== 消息已到达Handler，开始处理 =====");

        Channel currentChannel = ctx.channel();
        String sessionId = currentChannel.attr(SESSION_ID_KEY).get();

        System.out.println("【Handler】接收到解码后的消息VO：" + JSON.toJSONString(webSocketMsg)
                + "，sessionId=" + (sessionId == null ? "未知" : sessionId));

        String receiverId = webSocketMsg.getReceiverId();
        if (receiverId == null || receiverId.trim().isEmpty()) {
            System.err.println("ReceiverId为空，拒绝消息");
            return;
        }

        if (!RECEIVER_CHANNEL_MAP.containsKey(receiverId)) {
            boolean isValid = true;
            if (receiverIdService != null) {
                isValid = receiverIdService.validateReceiverId(receiverId);
            } else {
                System.out.println("调试阶段：receiverIdService未注入，跳过ReceiverId校验");
                isValid = true;
            }

            if (!isValid) {
                System.out.println("无效的ReceiverId，拒绝消息：" + receiverId);
                currentChannel.close();
                return;
            }

            RECEIVER_CHANNEL_MAP.put(receiverId, currentChannel);
            if (receiverIdService != null) {
                receiverIdService.markOnline(receiverId);
            }
            System.out.println("绑定ReceiverId与通道：" + receiverId + " -> " + currentChannel.id().asShortText());
        }

        if (webSocketMsg.getSendTime() == null) {
            webSocketMsg.setSendTime(new Date());
        }
        if (webSocketMsg.getMsgType() == null) {
            webSocketMsg.setMsgType("TEXT");
        }
        if (sessionId != null) {
            webSocketMsg.setSessionId(sessionId);
        }

        forwardMessage(webSocketMsg);

        System.out.println("处理客户端消息：" + DateUtil.formatDate(webSocketMsg.getSendTime(), "yyyy-MM-dd HH:mm:ss")
                + "，发送者：" + webSocketMsg.getUserId()
                + "，接收者：" + receiverId
                + "，内容：" + webSocketMsg.getMsgContent());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 你的原有代码（无需修改）
        Channel channel = ctx.channel();
        String sessionId = channel.attr(SESSION_ID_KEY).get();
        System.err.println("客户端通道异常：通道=" + channel.id().asShortText()
                + "，sessionId=" + (sessionId == null ? "未知" : sessionId)
                + "，异常信息：" + cause.getMessage());
        cause.printStackTrace();
        channel.close();
    }

    private void forwardMessage(WebSocketMsgVO webSocketMsg) {
        // 你的原有代码（无需修改）
        String targetReceiverId = webSocketMsg.getReceiverId();
        Channel targetChannel = RECEIVER_CHANNEL_MAP.get(targetReceiverId);

        if (targetChannel != null && targetChannel.isActive()) {
            String jsonMsg = JSON.toJSONString(webSocketMsg);
            targetChannel.writeAndFlush(new TextWebSocketFrame(jsonMsg));
            System.out.println("消息转发成功：接收者=" + targetReceiverId + "，消息=" + jsonMsg);
        } else {
            System.out.println("目标接收者离线，暂存离线消息：" + targetReceiverId);
        }
    }
}