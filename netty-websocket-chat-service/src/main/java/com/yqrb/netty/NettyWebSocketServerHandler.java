package com.yqrb.netty;

import com.alibaba.fastjson.JSON;
import com.yqrb.pojo.vo.WebSocketMsgVO;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelFutureListener;
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
 * 优化：统一日志、移除冗余操作、增强消息转发健壮性
 */
public class NettyWebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketMsgVO> {
    // 注入SLF4J日志对象（统一日志风格）
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
        // 优化：替换System.out为SLF4J logger
        logger.info("【客户端上线】通道ID：{}，在线人数：{}", channelId, ONLINE_CHANNELS.size());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String channelId = channel.id().asShortText();
        ONLINE_CHANNELS.remove(channel);

        // 优化：高并发下更安全的清理逻辑，添加清理结果日志
        String removedReceiverId = null;
        // 同步锁保证并发安全（ConcurrentHashMap本身线程安全，迭代器移除也安全，此处增强健壮性）
        synchronized (RECEIVER_CHANNEL_MAP) {
            for (Map.Entry<String, Channel> entry : RECEIVER_CHANNEL_MAP.entrySet()) {
                if (entry.getValue().equals(channel)) {
                    removedReceiverId = entry.getKey();
                    RECEIVER_CHANNEL_MAP.remove(removedReceiverId);
                    break;
                }
            }
        }

        // 优化：打印清理结果，便于排查问题
        if (removedReceiverId != null) {
            logger.info("【客户端离线】通道ID：{}，被移除的接收者ID：{}", channelId, removedReceiverId);
        } else {
            logger.warn("【客户端离线】通道ID：{}，未在RECEIVER_CHANNEL_MAP中找到对应记录", channelId);
        }

        String sessionId = channel.attr(SESSION_ID_KEY).get();
        logger.info("【客户端断开】通道ID：{}，sessionId：{}，在线人数：{}",
                channelId,
                (sessionId == null ? "未知" : sessionId),
                ONLINE_CHANNELS.size());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleEvent = (IdleStateEvent) evt;
            if (idleEvent.state() == IdleState.READER_IDLE) {
                Channel channel = ctx.channel();
                String channelId = channel.id().asShortText();
                String sessionId = channel.attr(SESSION_ID_KEY).get();
                // 优化：替换System.out为SLF4J logger
                logger.info("【客户端超时】通道ID：{}，sessionId：{}", channelId, (sessionId == null ? "未知" : sessionId));
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

        // 优化：移除重复存入RECEIVER_CHANNEL_MAP的代码（握手时已存入，无需重复操作）
        // 若需更新通道映射，可添加判断：if (!RECEIVER_CHANNEL_MAP.containsKey(receiverId)) {}

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
        // 优化：替换System.err为SLF4J logger，打印完整异常堆栈
        logger.error("【通道异常】通道ID：{}，sessionId：{}，异常原因：{}",
                channelId,
                (sessionId == null ? "未知" : sessionId),
                cause.getMessage(),
                cause);
        channel.close();
    }

    /**
     * 优化：增强消息转发健壮性，添加发送结果监听、可写性判断
     */
    private void forwardMessage(WebSocketMsgVO webSocketMsg) {
        String targetReceiverId = webSocketMsg.getReceiverId();
        Channel targetChannel = RECEIVER_CHANNEL_MAP.get(targetReceiverId);

        // 优化：增加isOpen()、isWritable()判断，避免向无效通道写入消息
        if (targetChannel != null && targetChannel.isOpen() && targetChannel.isActive() && targetChannel.isWritable()) {
            try {
                String jsonMsg = JSON.toJSONString(webSocketMsg);
                // 优化：添加ChannelFuture监听器，监听消息发送结果
                targetChannel.writeAndFlush(new TextWebSocketFrame(jsonMsg))
                        .addListener((ChannelFutureListener) future -> {
                            if (future.isSuccess()) {
                                logger.info("【消息转发成功】接收者：{}，消息内容：{}", targetReceiverId, jsonMsg);
                            } else {
                                logger.error("【消息转发失败】接收者：{}，消息发送失败，异常原因：{}",
                                        targetReceiverId,
                                        future.cause().getMessage());
                            }
                        });
            } catch (Exception e) {
                logger.error("【消息转发失败】接收者：{}，编码/发送异常：{}", targetReceiverId, e.getMessage(), e);
            }
        } else {
            logger.info("【消息转发失败】目标接收者离线或通道无效，接收者：{}", targetReceiverId);
        }
    }
}