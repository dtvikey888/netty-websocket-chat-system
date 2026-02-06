package com.yqrb.netty.pre;

import com.alibaba.fastjson.JSON;
import com.yqrb.netty.constant.NettyConstant;
import com.yqrb.pojo.vo.PreSaleChatMessageVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.PreSaleChatMessageService;
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
 * 售前Netty WebSocket业务处理器（完全独立，与售后解耦）
 */
public class PreSaleNettyWebSocketServerHandler extends SimpleChannelInboundHandler<PreSaleChatMessageVO> {
    private static final Logger logger = LoggerFactory.getLogger(PreSaleNettyWebSocketServerHandler.class);
    // 售前独立在线通道组
    private static final ChannelGroup ONLINE_CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    // 售前独立通道映射（核心隔离：与售后RECEIVER_CHANNEL_MAP完全独立）
    public static final Map<String, Channel> PRE_SALE_RECEIVER_CHANNEL_MAP = new ConcurrentHashMap<>();

    /**
     * 客户端上线
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        ONLINE_CHANNELS.add(channel);
        logger.info("【售前-客户端上线】通道ID：{}，在线人数：{}", channel.id().asShortText(), ONLINE_CHANNELS.size());
    }

    /**
     * 客户端离线
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        ONLINE_CHANNELS.remove(channel);
        // 清理售前通道映射
        String removedReceiverId = null;
        synchronized (PRE_SALE_RECEIVER_CHANNEL_MAP) {
            for (Map.Entry<String, Channel> entry : PRE_SALE_RECEIVER_CHANNEL_MAP.entrySet()) {
                if (entry.getValue().equals(channel)) {
                    removedReceiverId = entry.getKey();
                    PRE_SALE_RECEIVER_CHANNEL_MAP.remove(removedReceiverId);
                    break;
                }
            }
        }
        String receiverId = channel.attr(NettyConstant.PRE_SALE_RECEIVER_ID_KEY).get();
        logger.info("【售前-客户端离线】通道ID：{}，ReceiverId：{}，被清理ID：{}，在线人数：{}",
                channel.id().asShortText(),
                receiverId == null ? "未知" : receiverId,
                removedReceiverId == null ? "无" : removedReceiverId,
                ONLINE_CHANNELS.size());
    }

    /**
     * 心跳超时处理
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleEvent = (IdleStateEvent) evt;
            if (idleEvent.state() == IdleState.READER_IDLE) {
                Channel channel = ctx.channel();
                String receiverId = channel.attr(NettyConstant.PRE_SALE_RECEIVER_ID_KEY).get();
                logger.info("【售前-客户端超时】通道ID：{}，ReceiverId：{}，关闭通道", channel.id().asShortText(), receiverId);
                channel.close();
                return;
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    /**
     * 接收前端消息（核心：持久化+转发）
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PreSaleChatMessageVO msg) throws Exception {
        Channel currentChannel = ctx.channel();
        String channelId = currentChannel.id().asShortText();
        // 获取售前专属通道属性（核心隔离：使用PRE_SALE_xxx_KEY）
        String receiverId = currentChannel.attr(NettyConstant.PRE_SALE_RECEIVER_ID_KEY).get();
        String preSaleSessionId = currentChannel.attr(NettyConstant.PRE_SALE_SESSION_ID_KEY).get();
        String senderType = currentChannel.attr(NettyConstant.PRE_SALE_SENDER_TYPE_KEY).get();

        // 基础校验
        if (receiverId == null || preSaleSessionId == null) {
            logger.error("【售前-消息处理失败】通道未绑定核心属性，通道ID：{}", channelId);
            return;
        }
        if (msg.getReceiverId() == null || msg.getReceiverId().trim().isEmpty()) {
            logger.error("【售前-消息处理失败】目标ReceiverId为空，通道ID：{}", channelId);
            return;
        }

        // 补全消息默认值
        msg.setPreSaleSessionId(preSaleSessionId); // 绑定售前会话ID
        if (msg.getSenderId() == null) msg.setSenderId(receiverId); // 发送者ID=当前连接ID
        if (msg.getSenderType() == null) msg.setSenderType(senderType); // 绑定发送者类型
        if (msg.getSendTime() == null) msg.setSendTime(new Date()); // 补全发送时间
        if (msg.getMsgType() == null) msg.setMsgType(PreSaleChatMessageVO.MSG_TYPE_TEXT); // 默认文本
        if (msg.getIsRead() == null) msg.setIsRead(PreSaleChatMessageVO.IS_READ_NO); // 默认未读

        logger.info("【售前-消息接收】通道ID：{}，会话ID：{}，发送者：{}，接收者：{}，内容：{}",
                channelId, preSaleSessionId, msg.getSenderId(), msg.getReceiverId(), msg.getContent());

        // 1. 持久化消息到pre_sale_chat_message（售前独立表）
        persistPreSaleMessage(msg, receiverId);

        // 2. 转发消息到目标接收者（售前独立通道映射）
        forwardPreSaleMessage(msg);

        logger.info("【售前-消息处理完成】会话ID：{}，消息ID：{}", preSaleSessionId, msg.getMsgId());
    }

    /**
     * 持久化售前消息到数据库
     */
    private void persistPreSaleMessage(PreSaleChatMessageVO msg, String authReceiverId) {
        try {
            PreSaleChatMessageService service = SpringContextUtil.getBean(PreSaleChatMessageService.class);
            Result<Void> result = service.savePreSaleChatMessage(msg, authReceiverId);
            if (result.isSuccess()) {
                logger.info("【售前-消息持久化成功】会话ID：{}，消息ID：{}", msg.getPreSaleSessionId(), msg.getMsgId());
            } else {
                logger.error("【售前-消息持久化失败】会话ID：{}，错误：{}", msg.getPreSaleSessionId(), result.getMsg());
            }
        } catch (Exception e) {
            logger.error("【售前-消息持久化异常】会话ID：{}，异常：{}", msg.getPreSaleSessionId(), e.getMessage(), e);
        }
    }

    /**
     * 转发售前消息（核心：仅在预售通道映射中查询，会话ID一致性校验）
     */
    private void forwardPreSaleMessage(PreSaleChatMessageVO msg) {
        String targetReceiverId = msg.getReceiverId();
        String targetSessionId = msg.getPreSaleSessionId();
        // 从售前独立通道映射中查询（核心隔离）
        Channel targetChannel = PRE_SALE_RECEIVER_CHANNEL_MAP.get(targetReceiverId);

        // 通道有效性校验
        if (targetChannel == null || !targetChannel.isOpen() || !targetChannel.isActive()) {
            logger.info("【售前-消息转发失败】目标接收者离线，ReceiverId：{}，会话ID：{}", targetReceiverId, targetSessionId);
            return;
        }

        // 会话ID一致性校验（售前专属）
        String channelSessionId = targetChannel.attr(NettyConstant.PRE_SALE_SESSION_ID_KEY).get();
        if (channelSessionId == null || !channelSessionId.equals(targetSessionId)) {
            logger.warn("【售前-消息转发拦截】跨会话消息，通道会话ID：{}，消息会话ID：{}", channelSessionId, targetSessionId);
            return;
        }

        // 转发消息
        try {
            String jsonMsg = JSON.toJSONString(msg);
            targetChannel.writeAndFlush(new TextWebSocketFrame(jsonMsg))
                    .addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            logger.info("【售前-消息转发成功】接收者：{}，会话ID：{}", targetReceiverId, targetSessionId);
                        } else {
                            logger.error("【售前-消息转发失败】接收者：{}，异常：{}", targetReceiverId, future.cause().getMessage());
                        }
                    });
        } catch (Exception e) {
            logger.error("【售前-消息转发异常】接收者：{}，异常：{}", targetReceiverId, e.getMessage(), e);
        }
    }

    /**
     * 通道异常处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        String receiverId = channel.attr(NettyConstant.PRE_SALE_RECEIVER_ID_KEY).get();
        logger.error("【售前-通道异常】通道ID：{}，ReceiverId：{}，异常：{}",
                channel.id().asShortText(), receiverId, cause.getMessage(), cause);
        channel.close();
    }
}