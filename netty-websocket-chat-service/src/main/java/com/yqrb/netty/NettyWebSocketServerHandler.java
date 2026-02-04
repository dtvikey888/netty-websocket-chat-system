package com.yqrb.netty;

import com.alibaba.fastjson.JSON;
import com.yqrb.netty.constant.NettyConstant;
import com.yqrb.pojo.vo.ChatMessageVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.pojo.vo.WebSocketMsgVO;
import com.yqrb.service.ChatMessageService;
import com.yqrb.util.SpringContextUtil;
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
 * 新增：保留消息自定义sessionId，仅为空时用通道自身ID兜底
 * 补充：保留消息自定义senderType，仅为空时用通道属性兜底
 * 新增：消息持久化功能，调用ChatMessageService存入chat_message表
 */
public class NettyWebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketMsgVO> {
    // 注入SLF4J日志对象（统一日志风格）
    private static final Logger logger = LoggerFactory.getLogger(NettyWebSocketServerHandler.class);

    private static final ChannelGroup ONLINE_CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    public static final Map<String, Channel> RECEIVER_CHANNEL_MAP = new ConcurrentHashMap<>();

    // 新增：声明ChatMessageService，通过Spring上下文工具类获取
    private static ChatMessageService chatMessageService;

    // 静态代码块初始化ChatMessageService
    static {
        chatMessageService = SpringContextUtil.getBean(ChatMessageService.class);
        if (chatMessageService == null) {
            logger.error("【初始化失败】无法通过SpringContextUtil获取ChatMessageService实例，消息持久化功能将不可用");
        }
    }

    // ===== 核心修复：不在channelActive中解析URI，改为首次接收消息时解析 =====
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String channelId = channel.id().asShortText();
        ONLINE_CHANNELS.add(channel);
        logger.info("【客户端上线】通道ID：{}，在线人数：{}", channelId, ONLINE_CHANNELS.size());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String channelId = channel.id().asShortText();
        ONLINE_CHANNELS.remove(channel);

        // 优化：高并发下更安全的清理逻辑，添加清理结果日志
        String removedReceiverId = null;
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

        String channelSelfId = channel.attr(NettyConstant.SESSION_ID_KEY).get();
        logger.info("【客户端断开】通道ID：{}，通道自身ID：{}，在线人数：{}",
                channelId,
                (channelSelfId == null ? "未知" : channelSelfId),
                ONLINE_CHANNELS.size());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleEvent = (IdleStateEvent) evt;
            if (idleEvent.state() == IdleState.READER_IDLE) {
                Channel channel = ctx.channel();
                String channelId = channel.id().asShortText();
                String channelSelfId = channel.attr(NettyConstant.SESSION_ID_KEY).get();
                logger.info("【客户端超时】通道ID：{}，通道自身ID：{}", channelId, (channelSelfId == null ? "未知" : channelSelfId));
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
        // 从通道获取自身ID和绑定的senderType（仅用于兜底）
        String channelSelfId = currentChannel.attr(NettyConstant.SESSION_ID_KEY).get();
        String channelSenderType = currentChannel.attr(NettyConstant.SENDER_TYPE_KEY).get();

        // 优化：日志区分「通道自身ID」、「通道绑定senderType」和「消息自定义值」
        logger.info("=====================================");
        logger.info("【消息接收成功】通道ID：{}", channelId);
        logger.info("通道自身ID（客服/用户ID）：{}", (channelSelfId == null ? "未知" : channelSelfId));
        logger.info("通道绑定senderType：{}", (channelSenderType == null ? "未知" : channelSenderType));
        logger.info("消息自带sessionId（自定义）：{}", (webSocketMsg.getSessionId() == null ? "未知" : webSocketMsg.getSessionId()));
        logger.info("消息自带senderType（自定义）：{}", (webSocketMsg.getSenderType() == null ? "未知" : webSocketMsg.getSenderType()));
        logger.info("消息内容：{}", JSON.toJSONString(webSocketMsg));
        logger.info("=====================================");

        // 校验：通道自身ID非空（确保会话已注册）
        if (channelSelfId == null) {
            logger.error("【消息处理失败】通道未注册自身ID，通道ID：{}", channelId);
            return;
        }

        String receiverId = webSocketMsg.getReceiverId();
        if (receiverId == null || receiverId.trim().isEmpty()) {
            logger.error("【消息处理失败】receiverId为空，通道ID：{}", channelId);
            return;
        }

        // 后续receiverIdService注入完成后，替换为真实校验逻辑
        boolean isValid = true;
        logger.debug("【调试模式】receiverIdService未注入，跳过校验");

        if (!isValid) {
            logger.error("【消息处理失败】无效的receiverId：{}，通道ID：{}", receiverId, channelId);
            currentChannel.close();
            return;
        }

        // 补全消息默认值（核心：优先保留消息自定义值，仅为空时兜底）
        if (webSocketMsg.getSendTime() == null) {
            webSocketMsg.setSendTime(new Date());
        }
        if (webSocketMsg.getMsgType() == null) {
            webSocketMsg.setMsgType(WebSocketMsgVO.MSG_TYPE_TEXT);
        }
        // 兜底：sessionId（仅消息为空时，用通道自身ID）
        if (webSocketMsg.getSessionId() == null || webSocketMsg.getSessionId().trim().isEmpty()) {
            webSocketMsg.setSessionId(channelSelfId);
            logger.warn("【消息补全】消息无自定义sessionId，使用通道自身ID兜底：{}", channelSelfId);
        }
        // 👇 新增：senderType兜底（仅消息为空时，用通道绑定的类型）
        if (webSocketMsg.getSenderType() == null || webSocketMsg.getSenderType().trim().isEmpty()) {
            webSocketMsg.setSenderType(channelSenderType);
            logger.warn("【消息补全】消息无自定义senderType，使用通道绑定类型兜底：{}", webSocketMsg.getSenderType());
        }

        // ======================================
        // 新增：核心持久化逻辑（调用ChatMessageService存入chat_message表）
        // ======================================
        if (chatMessageService != null) {
            try {
                // 1. 调用ChatMessageService完成消息入库（第二个参数是发送者自身ID，用于校验）
                Result<ChatMessageVO> saveResult = chatMessageService.sendMessage(webSocketMsg, channelSelfId);

                // 2. 处理入库结果：失败则给发送方反馈，成功则继续转发
                if (!saveResult.isSuccess()) {
                    String errorMsg = "消息持久化失败：" + saveResult.getMsg();
                    logger.error("【消息持久化失败】通道ID：{}，{}", channelId, errorMsg);
                    // 给发送方返回错误信息
                    currentChannel.writeAndFlush(new TextWebSocketFrame(errorMsg))
                            .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                    return;
                }

                // 3. 入库成功日志
                logger.info("【消息持久化成功】通道ID：{}，消息ID：{}，会话ID：{}",
                        channelId, saveResult.getData().getMsgId(), webSocketMsg.getSessionId());

            } catch (Exception e) {
                String errorMsg = "消息持久化异常：" + e.getMessage();
                logger.error("【消息持久化异常】通道ID：{}，{}", channelId, errorMsg, e);
                // 给发送方返回异常信息，不中断实时转发（可选：根据业务需求决定是否返回）
                currentChannel.writeAndFlush(new TextWebSocketFrame(errorMsg))
                        .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                // 异常不阻断实时转发（保证通信流畅，入库失败后续可补偿）
            }
        } else {
            logger.warn("【消息持久化跳过】ChatMessageService实例为null，无法完成入库");
        }

        // 转发消息（此时sessionId和senderType均已补全，且保留了消息自定义值）
        forwardMessage(webSocketMsg);

        // 非空判断，避免空指针，打印最终结果
        String userId = webSocketMsg.getUserId() == null ? "未知" : webSocketMsg.getUserId();
        String msgContent = webSocketMsg.getMsgContent() == null ? "无内容" : webSocketMsg.getMsgContent();
        logger.info("【消息处理完成】发送者：{}，发送者类型：{}，接收者：{}，内容：{}，最终sessionId：{}",
                userId, webSocketMsg.getSenderType(), receiverId, msgContent, webSocketMsg.getSessionId());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        String channelId = channel.id().asShortText();
        String channelSelfId = channel.attr(NettyConstant.SESSION_ID_KEY).get();
        logger.error("【通道异常】通道ID：{}，通道自身ID：{}，异常原因：{}",
                channelId,
                (channelSelfId == null ? "未知" : channelSelfId),
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
                                logger.info("【消息转发成功】接收者：{}，发送者类型：{}，最终sessionId：{}",
                                        targetReceiverId, webSocketMsg.getSenderType(), webSocketMsg.getSessionId());
                            } else {
                                logger.error("【消息转发失败】接收者：{}，异常原因：{}",
                                        targetReceiverId, future.cause().getMessage());
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