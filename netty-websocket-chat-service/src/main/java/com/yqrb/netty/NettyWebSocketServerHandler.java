package com.yqrb.netty;

import com.alibaba.fastjson.JSON;
import com.yqrb.netty.constant.NettyConstant; // 新增：导入公共常量类
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
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 修复：调整URI解析时机，解决channelActive中URI为null的问题
 * 优化：统一日志、移除冗余操作、增强消息转发健壮性、整合公共常量类
 */
public class NettyWebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketMsgVO> {
    // 注入SLF4J日志对象（统一日志风格）
    private static final Logger logger = LoggerFactory.getLogger(NettyWebSocketServerHandler.class);

    private static final ChannelGroup ONLINE_CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    public static final Map<String, Channel> RECEIVER_CHANNEL_MAP = new ConcurrentHashMap<>();
    // 移除：删除本地冗余的SESSION_ID_KEY定义，改用公共常量类中的定义

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

        // 修改：使用公共常量类中的SESSION_ID_KEY获取sessionId
        String sessionId = channel.attr(NettyConstant.SESSION_ID_KEY).get();
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
                // 修改：使用公共常量类中的SESSION_ID_KEY获取sessionId
                String sessionId = channel.attr(NettyConstant.SESSION_ID_KEY).get();
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
        // 修改：使用公共常量类中的SESSION_ID_KEY获取sessionId
        String sessionId = currentChannel.attr(NettyConstant.SESSION_ID_KEY).get();

        // 使用SLF4J打印info级别日志，确认消息到达
        logger.info("=====================================");
        logger.info("【消息接收成功】通道ID：{}", channelId);
        logger.info("sessionId：{}", (sessionId == null ? "未知" : sessionId));
        logger.info("消息内容：{}", JSON.toJSONString(webSocketMsg));
        logger.info("=====================================");

        // 移除：删除重复的首次消息会话绑定逻辑（握手时已完成）
        // 新增：首次接收消息，若未绑定sessionId则尝试从消息中提取并绑定（流程闭环关键）
//        if (sessionId == null) {
//            String newSessionId = webSocketMsg.getSessionId();
//            String newReceiverId = webSocketMsg.getReceiverId();
//            if (newSessionId != null && !newSessionId.trim().isEmpty()) {
//                bindSessionInfo(ctx, newSessionId, newReceiverId);
//                // 重新获取绑定后的sessionId
//                sessionId = currentChannel.attr(NettyConstant.SESSION_ID_KEY).get();
//            } else {
//                logger.error("【消息处理失败】通道未注册会话ID，且消息中无有效会话参数，通道ID：{}", channelId);
//                return;
//            }
//        }

        // 已有sessionId（绑定成功），继续处理消息
        if (sessionId == null) {
            logger.error("【消息处理失败】通道未注册会话ID，通道ID：{}", channelId);
            return;
        }

        String receiverId = webSocketMsg.getReceiverId();
        if (receiverId == null || receiverId.trim().isEmpty()) {
            logger.error("【消息处理失败】receiverId为空，通道ID：{}", channelId);
            return;
        }

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
            // 修改：使用WebSocketMsgVO中的常量，避免硬编码
            webSocketMsg.setMsgType(WebSocketMsgVO.MSG_TYPE_TEXT);
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
        // 修改：使用公共常量类中的SESSION_ID_KEY获取sessionId
        String sessionId = channel.attr(NettyConstant.SESSION_ID_KEY).get();
        // 优化：替换System.err为SLF4J logger，打印完整异常堆栈
        logger.error("【通道异常】通道ID：{}，sessionId：{}，异常原因：{}",
                channelId,
                (sessionId == null ? "未知" : sessionId),
                cause.getMessage(),
                cause);
        channel.close();
    }

    /**
     * 新增：绑定会话信息到Channel（会话建立时调用，使用公共常量类）
     * @param ctx 通道上下文
     * @param sessionId 会话ID
     * @param receiverId 接收者ID（客服/用户）
     */
    private void bindSessionInfo(ChannelHandlerContext ctx, String sessionId, String receiverId) {
        Channel channel = ctx.channel();
        // 1. 绑定sessionId到Channel属性（使用公共常量类）
        channel.attr(NettyConstant.SESSION_ID_KEY).set(sessionId);
        // 2. 绑定receiverId到Channel上下文（使用公共常量类）
        channel.attr(NettyConstant.RECEIVER_ID_KEY).set(receiverId);
        // 3. 绑定默认发送者类型（可选，补充完整）
        channel.attr(NettyConstant.SENDER_TYPE_KEY).set(WebSocketMsgVO.SENDER_TYPE_USER);
        // 4. 存入接收者-通道映射（高并发安全，先移除旧映射再新增）
        if (receiverId != null && !receiverId.trim().isEmpty()) {
            synchronized (RECEIVER_CHANNEL_MAP) {
                // 移除该接收者对应的旧通道（避免重复映射）
                RECEIVER_CHANNEL_MAP.remove(receiverId);
                // 新增最新通道映射
                RECEIVER_CHANNEL_MAP.put(receiverId, channel);
            }
            logger.info("【会话绑定成功】通道ID：{}，sessionId：{}，receiverId：{}",
                    channel.id().asShortText(), sessionId, receiverId);
        } else {
            logger.error("【会话绑定失败】接收者ID为空，通道ID：{}", channel.id().asShortText());
        }
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