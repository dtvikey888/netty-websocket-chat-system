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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 修复：调整URI解析时机，解决channelActive中URI为null的问题
 */
public class NettyWebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketMsgVO> {
    // 注入日志对象（在类中定义）
    private static final Logger logger = LoggerFactory.getLogger(NettyWebSocketServerHandler.class);

    private static final ChannelGroup ONLINE_CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    public static final Map<String, Channel> RECEIVER_CHANNEL_MAP = new ConcurrentHashMap<>();
    public static final AttributeKey<String> SESSION_ID_KEY = AttributeKey.valueOf("SESSION_ID");

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
        String sessionId = currentChannel.attr(SESSION_ID_KEY).get();

        // 使用SLF4J打印info级别日志，确认消息到达
        logger.info("=====================================");
        logger.info("【消息接收成功】通道ID：{}", channelId);
        logger.info("sessionId：{}", (sessionId == null ? "未知" : sessionId));
        logger.info("消息内容：{}", JSON.toJSONString(webSocketMsg));
        logger.info("=====================================");

        // 已有sessionId（握手时已注册），直接处理消息
        if (sessionId == null) {
            logger.error("【消息处理失败】通道未注册会话ID，通道ID：{}", channelId);
            return;
        }

        String receiverId = webSocketMsg.getReceiverId();
        if (receiverId == null || receiverId.trim().isEmpty()) {
            logger.error("【消息处理失败】receiverId为空，通道ID：{}", channelId);
            return;
        }

        // ========== 补充1：存入RECEIVER_CHANNEL_MAP + 即时打印 ==========
        RECEIVER_CHANNEL_MAP.put(receiverId, currentChannel);
        // 打印存入结果和当前Map状态
        logger.info("【RECEIVER_CHANNEL_MAP 存入成功】→ 纯净接收者ID：{}，通道ID：{}", receiverId, channelId);
        logger.info("【当前Map状态】→ 总记录数：{}，所有接收者ID：{}",
                RECEIVER_CHANNEL_MAP.size(),
                RECEIVER_CHANNEL_MAP.keySet()); // 打印所有已存入的receiverId

        // 后续receiverIdService注入完成后，替换为真实校验逻辑
        // boolean isValid = receiverIdService.isValid(receiverId);
        boolean isValid = true;
        logger.debug("【调试模式】receiverIdService未注入，跳过校验");

        if (!isValid) {
            logger.error("【消息处理失败】无效的receiverId：{}，通道ID：{}", receiverId, channelId);
            currentChannel.close();
            return;
        }

        // 补全消息默认值
        if (webSocketMsg.getSendTime() == null) {
            webSocketMsg.setSendTime(new Date());
        }
        if (webSocketMsg.getMsgType() == null) {
            webSocketMsg.setMsgType("TEXT");
        }
        webSocketMsg.setSessionId(sessionId);

        // 转发消息（此时Map中已有数据，转发时可查询）
        forwardMessage(webSocketMsg);

        // 非空判断，避免空指针
        String userId = webSocketMsg.getUserId() == null ? "未知" : webSocketMsg.getUserId();
        String msgContent = webSocketMsg.getMsgContent() == null ? "无内容" : webSocketMsg.getMsgContent();
        logger.info("【消息处理完成】发送者：{}，接收者：{}，内容：{}", userId, receiverId, msgContent);
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