package com.yqrb.netty.pre;

import com.alibaba.fastjson.JSON;
import com.yqrb.netty.constant.NettyConstant;
import com.yqrb.pojo.vo.PreSaleChatMessageVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.PreSaleChatMessageService;
import com.yqrb.util.RedisUtil;
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
import org.springframework.core.env.Environment;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PreSaleNettyWebSocketServerHandler extends SimpleChannelInboundHandler<PreSaleChatMessageVO> {
    private static final Logger logger = LoggerFactory.getLogger(PreSaleNettyWebSocketServerHandler.class);
    private static final ChannelGroup ONLINE_CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    public static final Map<String, Channel> PRE_SALE_RECEIVER_CHANNEL_MAP = new ConcurrentHashMap<>();

    // 离线消息Redis前缀（从配置读取）
    private static String OFFLINE_MSG_PREFIX;
    // Redis工具类（通过SpringContextUtil获取）
    private static RedisUtil redisUtil;

    static {
        // 初始化静态属性
        try {
            Environment env = SpringContextUtil.getBean(Environment.class);
            OFFLINE_MSG_PREFIX = env.getProperty("custom.receiver.session.offline-msg-prefix", "newspaper:offline:msg:");
            redisUtil = SpringContextUtil.getBean(RedisUtil.class);
        } catch (Exception e) {
            logger.warn("【售前-静态初始化】Redis配置加载失败，离线消息功能不可用：{}", e.getMessage());
            OFFLINE_MSG_PREFIX = "newspaper:offline:msg:";
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        ONLINE_CHANNELS.add(channel);
        String receiverId = channel.attr(NettyConstant.PRE_SALE_RECEIVER_ID_KEY).get();
        logger.info("【售前-客户端上线】通道ID：{}，ReceiverId：{}，在线人数：{}",
                channel.id().asShortText(), receiverId, ONLINE_CHANNELS.size());

        // 上线后推送离线消息
        if (receiverId != null) {
            pushOfflineMessage(channel, receiverId);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        ONLINE_CHANNELS.remove(channel);
        String receiverId = channel.attr(NettyConstant.PRE_SALE_RECEIVER_ID_KEY).get();

        // 修复：清理通道映射（更高效）
        if (receiverId != null) {
            PRE_SALE_RECEIVER_CHANNEL_MAP.remove(receiverId);
        }

        logger.info("【售前-客户端离线】通道ID：{}，ReceiverId：{}，在线人数：{}，剩余通道映射数：{}",
                channel.id().asShortText(), receiverId, ONLINE_CHANNELS.size(), PRE_SALE_RECEIVER_CHANNEL_MAP.size());
    }

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

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PreSaleChatMessageVO msg) throws Exception {
        Channel currentChannel = ctx.channel();
        String channelId = currentChannel.id().asShortText();

        // 获取通道属性
        String senderReceiverId = currentChannel.attr(NettyConstant.PRE_SALE_RECEIVER_ID_KEY).get();
        String preSaleSessionId = currentChannel.attr(NettyConstant.PRE_SALE_SESSION_ID_KEY).get();
        String senderType = currentChannel.attr(NettyConstant.PRE_SALE_SENDER_TYPE_KEY).get();

        // 基础校验
        if (senderReceiverId == null || preSaleSessionId == null) {
            logger.error("【售前-消息处理失败】通道未绑定核心属性，通道ID：{}，senderReceiverId：{}，sessionId：{}",
                    channelId, senderReceiverId, preSaleSessionId);
            return;
        }
        if (msg.getReceiverId() == null || msg.getReceiverId().trim().isEmpty()) {
            logger.error("【售前-消息处理失败】目标ReceiverId为空，通道ID：{}，消息内容：{}", channelId, msg.getContent());
            return;
        }

        // 补全消息默认值
        msg.setPreSaleSessionId(preSaleSessionId);
        if (msg.getSenderId() == null) msg.setSenderId(senderReceiverId);
        if (msg.getSenderType() == null) msg.setSenderType(senderType);
        if (msg.getSendTime() == null) msg.setSendTime(new Date());
        if (msg.getMsgType() == null) msg.setMsgType(PreSaleChatMessageVO.MSG_TYPE_TEXT);
        if (msg.getIsRead() == null) msg.setIsRead(PreSaleChatMessageVO.IS_READ_NO);

        logger.info("【售前-消息接收】通道ID：{}，会话ID：{}，发送者：{}，接收者：{}，内容：{}",
                channelId, preSaleSessionId, msg.getSenderId(), msg.getReceiverId(), msg.getContent());

        // 1. 持久化消息
        persistPreSaleMessage(msg, senderReceiverId);

        // 2. 转发消息（核心修复：增加离线兜底）
        forwardPreSaleMessage(msg);

        logger.info("【售前-消息处理完成】会话ID：{}，消息ID：{}", preSaleSessionId, msg.getMsgId());
    }

    // 持久化消息
    private void persistPreSaleMessage(PreSaleChatMessageVO msg, String authReceiverId) {
        try {
            PreSaleChatMessageService service = SpringContextUtil.getBean(PreSaleChatMessageService.class);

            // 修复：拼接R_FIXED_0000_前缀，适配ReceiverIdService校验
            String realAuthReceiverId = "R_FIXED_0000_" + authReceiverId;
            Result<Void> result = service.savePreSaleChatMessage(msg, realAuthReceiverId);

            if (result.isSuccess()) {
                logger.info("【售前-消息持久化成功】会话ID：{}，消息ID：{}", msg.getPreSaleSessionId(), msg.getMsgId());
            } else {
                logger.error("【售前-消息持久化失败】会话ID：{}，消息ID：{}，错误码：{}，错误信息：{}",
                        msg.getPreSaleSessionId(), msg.getMsgId(), result.getCode(), result.getMsg());
                // 重试：使用原始ReceiverId
                Result<Void> retryResult = service.savePreSaleChatMessage(msg, authReceiverId);
                if (retryResult.isSuccess()) {
                    logger.info("【售前-消息持久化重试成功】会话ID：{}，消息ID：{}", msg.getPreSaleSessionId(), msg.getMsgId());
                } else {
                    logger.error("【售前-消息持久化重试失败】会话ID：{}，消息ID：{}，错误信息：{}",
                            msg.getPreSaleSessionId(), msg.getMsgId(), retryResult.getMsg());
                }
            }
        } catch (Exception e) {
            logger.error("【售前-消息持久化异常】会话ID：{}，消息ID：{}，异常：{}",
                    msg.getPreSaleSessionId(), msg.getMsgId(), e.getMessage(), e);
        }
    }

    // 转发消息（核心：增加离线消息存储）
    private void forwardPreSaleMessage(PreSaleChatMessageVO msg) {
        String targetReceiverId = msg.getReceiverId();
        String targetSessionId = msg.getPreSaleSessionId();

        // 1. 获取目标通道
        Channel targetChannel = PRE_SALE_RECEIVER_CHANNEL_MAP.get(targetReceiverId);
        logger.info("【售前-消息转发】目标ReceiverId：{}，通道是否存在：{}，在线通道数：{}",
                targetReceiverId, targetChannel != null, PRE_SALE_RECEIVER_CHANNEL_MAP.size());

        // 2. 通道有效则直接转发
        if (targetChannel != null && targetChannel.isOpen() && targetChannel.isActive() && targetChannel.isWritable()) {
            // 绑定会话ID（如果未绑定）
            String channelSessionId = targetChannel.attr(NettyConstant.PRE_SALE_SESSION_ID_KEY).get();
            if (channelSessionId == null || !channelSessionId.equals(targetSessionId)) {
                targetChannel.attr(NettyConstant.PRE_SALE_SESSION_ID_KEY).set(targetSessionId);
                logger.info("【售前-消息转发】为目标通道绑定会话ID：{}", targetSessionId);
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
                                // 转发失败则存储离线消息
                                saveOfflineMessage(targetReceiverId, msg);
                            }
                        });
            } catch (Exception e) {
                logger.error("【售前-消息转发异常】接收者：{}，异常：{}", targetReceiverId, e.getMessage(), e);
                saveOfflineMessage(targetReceiverId, msg);
            }
        } else {
            // 3. 通道无效则存储离线消息
            logger.info("【售前-消息转发】目标接收者离线，存储离线消息：{}", targetReceiverId);
            saveOfflineMessage(targetReceiverId, msg);
        }
    }

    // 存储离线消息到Redis
    private void saveOfflineMessage(String receiverId, PreSaleChatMessageVO msg) {
        try {
            if (redisUtil == null) {
                logger.warn("【售前-离线消息】Redis工具类未初始化，无法存储离线消息");
                return;
            }
            String redisKey = OFFLINE_MSG_PREFIX + receiverId;
            redisUtil.lSet(redisKey, JSON.toJSONString(msg), 86400); // 24小时过期
            logger.info("【售前-离线消息存储成功】ReceiverId：{}，消息ID：{}", receiverId, msg.getMsgId());
        } catch (Exception e) {
            logger.error("【售前-离线消息存储失败】ReceiverId：{}，异常：{}", receiverId, e.getMessage(), e);
        }
    }

    // 推送离线消息
    private void pushOfflineMessage(Channel channel, String receiverId) {
        try {
            if (redisUtil == null) {
                return;
            }
            String redisKey = OFFLINE_MSG_PREFIX + receiverId;
            if (!redisUtil.hasKey(redisKey)) {
                return;
            }

            // 获取所有离线消息
            long msgCount = redisUtil.lGetListSize(redisKey);
            if (msgCount == 0) {
                return;
            }

            logger.info("【售前-离线消息推送】ReceiverId：{}，离线消息数：{}", receiverId, msgCount);
            for (int i = 0; i < msgCount; i++) {
                String msgStr = (String) redisUtil.lIndex(redisKey, i);
                PreSaleChatMessageVO msg = JSON.parseObject(msgStr, PreSaleChatMessageVO.class);
                channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(msg)));
            }

            // 删除已推送的离线消息
            redisUtil.delete(redisKey);
            logger.info("【售前-离线消息推送完成】ReceiverId：{}，共推送{}条", receiverId, msgCount);
        } catch (Exception e) {
            logger.error("【售前-离线消息推送异常】ReceiverId：{}，异常：{}", receiverId, e.getMessage(), e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        String receiverId = channel.attr(NettyConstant.PRE_SALE_RECEIVER_ID_KEY).get();
        logger.error("【售前-通道异常】通道ID：{}，ReceiverId：{}，异常：{}",
                channel.id().asShortText(), receiverId, cause.getMessage(), cause);
        channel.close();
    }
}