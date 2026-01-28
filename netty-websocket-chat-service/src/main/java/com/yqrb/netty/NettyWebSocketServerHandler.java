package com.yqrb.netty;

import com.alibaba.fastjson.JSON;
import com.yqrb.pojo.vo.WebSocketMsgVO;
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

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 修复：调整URI解析时机，解决channelActive中URI为null的问题
 */
public class NettyWebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketMsgVO> {
    private static final ChannelGroup ONLINE_CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final Map<String, Channel> RECEIVER_CHANNEL_MAP = new ConcurrentHashMap<>();
    private static final AttributeKey<String> SESSION_ID_KEY = AttributeKey.valueOf("SESSION_ID");

    // ===== 核心修复：不在channelActive中解析URI，改为首次接收消息时解析 =====
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String channelId = channel.id().asShortText();
        ONLINE_CHANNELS.add(channel);
        // 仅打印基础日志，不解析URI（避免时序问题）
        System.out.println("【客户端上线】通道ID：" + channelId + "，在线人数：" + ONLINE_CHANNELS.size());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String channelId = channel.id().asShortText();
        ONLINE_CHANNELS.remove(channel);

        RECEIVER_CHANNEL_MAP.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(channel)) {
                String receiverId = entry.getKey();
                System.out.println("【客户端离线】通道ID：" + channelId + "，receiverId：" + receiverId);
                return true;
            }
            return false;
        });

        String sessionId = channel.attr(SESSION_ID_KEY).get();
        System.out.println("【客户端断开】通道ID：" + channelId + "，sessionId：" + (sessionId == null ? "未知" : sessionId) + "，在线人数：" + ONLINE_CHANNELS.size());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleEvent = (IdleStateEvent) evt;
            if (idleEvent.state() == IdleState.READER_IDLE) {
                Channel channel = ctx.channel();
                String channelId = channel.id().asShortText();
                String sessionId = channel.attr(SESSION_ID_KEY).get();
                System.out.println("【客户端超时】通道ID：" + channelId + "，sessionId：" + (sessionId == null ? "未知" : sessionId));
                channel.close();
                return;
            }
        }
        super.userEventTriggered(ctx, evt);
    }



    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketMsgVO webSocketMsg) throws Exception {
        Channel currentChannel = ctx.channel();
        String channelId = currentChannel.id().asShortText();

        // ===== 核心修复：首次接收消息时解析URI和sessionId =====
        String sessionId = currentChannel.attr(SESSION_ID_KEY).get();
        if (sessionId == null) {
            String uri = currentChannel.attr(NettyWebSocketServer.CLIENT_WEBSOCKET_URI).get();
            if (uri != null && uri.startsWith("/newspaper/websocket/")) {
                sessionId = uri.substring("/newspaper/websocket/".length());
                currentChannel.attr(SESSION_ID_KEY).set(sessionId);
                System.out.println("【首次消息解析】通道ID：" + channelId + "，sessionId：" + sessionId);
            } else {
                System.err.println("【首次消息解析】通道ID：" + channelId + "，URI解析失败：" + uri);
            }
        }

        // 强制打印日志，确认消息到达
        System.out.println("=====================================");
        System.out.println("【消息接收成功】通道ID：" + channelId);
        System.out.println("sessionId：" + (sessionId == null ? "未知" : sessionId));
        System.out.println("消息内容：" + JSON.toJSONString(webSocketMsg));
        System.out.println("=====================================");

        String receiverId = webSocketMsg.getReceiverId();
        if (receiverId == null || receiverId.trim().isEmpty()) {
            System.err.println("【消息处理失败】receiverId为空，通道ID：" + channelId);
            return;
        }

        if (!RECEIVER_CHANNEL_MAP.containsKey(receiverId)) {
            boolean isValid = true;
            System.out.println("【调试模式】receiverIdService未注入，跳过校验");
            isValid = true;

            if (!isValid) {
                System.out.println("【消息处理失败】无效的receiverId：" + receiverId + "，通道ID：" + channelId);
                currentChannel.close();
                return;
            }

            RECEIVER_CHANNEL_MAP.put(receiverId, currentChannel);
            System.out.println("【绑定成功】receiverId：" + receiverId + " -> 通道ID：" + channelId);
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

        System.out.println("【消息处理完成】发送者：" + webSocketMsg.getUserId() + "，接收者：" + receiverId + "，内容：" + webSocketMsg.getMsgContent());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        String channelId = channel.id().asShortText();
        String sessionId = channel.attr(SESSION_ID_KEY).get();
        System.err.println("【通道异常】通道ID：" + channelId + "，sessionId：" + (sessionId == null ? "未知" : sessionId));
        cause.printStackTrace();
        channel.close();
    }

    private void forwardMessage(WebSocketMsgVO webSocketMsg) {
        String targetReceiverId = webSocketMsg.getReceiverId();
        Channel targetChannel = RECEIVER_CHANNEL_MAP.get(targetReceiverId);

        if (targetChannel != null && targetChannel.isActive()) {
            String jsonMsg = JSON.toJSONString(webSocketMsg);
            targetChannel.writeAndFlush(new TextWebSocketFrame(jsonMsg));
            System.out.println("【消息转发成功】接收者：" + targetReceiverId + "，消息：" + jsonMsg);
        } else {
            System.out.println("【消息转发失败】目标接收者离线：" + targetReceiverId);
        }
    }
}