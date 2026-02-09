package com.yqrb.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson.JSON;
import com.yqrb.mapper.PreSaleChatMessageMapper;
import com.yqrb.netty.NettyWebSocketServerHandler;
import com.yqrb.netty.constant.NettyConstant;
import com.yqrb.netty.pre.PreSaleNettyWebSocketServerHandler;
import com.yqrb.pojo.po.PreSaleChatMessagePO;
import com.yqrb.pojo.vo.PreSaleChatMessageVO;
import com.yqrb.pojo.vo.PreSaleWebSocketMsgVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.pojo.vo.ResultCode;
import com.yqrb.service.PreSaleChatMessageService;
import com.yqrb.service.ReceiverIdService;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 售前咨询聊天记录Service实现类
 */
@Service
public class PreSaleChatMessageServiceImpl implements PreSaleChatMessageService {
    private static final Logger logger = LoggerFactory.getLogger(PreSaleChatMessageServiceImpl.class);

    @Resource
    private PreSaleChatMessageMapper preSaleChatMessageMapper;

    @Resource
    private ReceiverIdService receiverIdService;

    @Override
    public Result<Void> wsReconnectPushUnread(String sessionId, String receiverId) {
        // 1. 参数校验：sessionId + receiverId 均非空
        if (!StringUtils.hasText(sessionId)) {
            return Result.paramError("sessionId不能为空");
        }
        if (!StringUtils.hasText(receiverId)) {
            return Result.paramError("receiverId不能为空");
        }

        try {
            // 2. 按sessionId + receiverId 查询售前未读消息（核心：适配售前表）
            List<PreSaleChatMessagePO> unreadMsgPOList = preSaleChatMessageMapper.listUnreadBySessionIdAndReceiverId(sessionId, receiverId);
            if (CollectionUtils.isEmpty(unreadMsgPOList)) {
                // 无未读消息，自定义成功提示（泛型兼容Void）
                return Result.custom(ResultCode.SUCCESS, "售前重连成功，当前会话无未读消息");
            }

            // 3. 查找售前Netty通道（优先按receiverId，兜底按sessionId）
            Channel targetChannel = PreSaleNettyWebSocketServerHandler.PRE_SALE_RECEIVER_CHANNEL_MAP.get(receiverId);
            if (targetChannel == null) {
                targetChannel = findPreSaleChannelBySessionId(sessionId);
            }
            if (targetChannel == null || !targetChannel.isActive() || !targetChannel.isWritable()) {
                return Result.error("售前重连成功，但会话通道未在线，未读消息将在通道上线后自动推送");
            }

            // 4. 批量推送（适配售前WebSocketMsgVO）
            for (PreSaleChatMessagePO po : unreadMsgPOList) {
                PreSaleWebSocketMsgVO wsMsg = new PreSaleWebSocketMsgVO();
                wsMsg.setSessionId(po.getPreSaleSessionId());
                wsMsg.setReceiverId(po.getReceiverId()); // 限定售前接收方
                wsMsg.setUserId(po.getSenderId());
                wsMsg.setMsgContent(po.getContent());
                wsMsg.setMsgType(po.getMsgType());
                wsMsg.setSendTime(po.getSendTime());
                wsMsg.setSenderType(po.getSenderType());

                targetChannel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(wsMsg)));
            }

            logger.info("【售前WebSocket重连】sessionId：{}，receiverId：{}，成功推送{}条未读消息",
                    sessionId, receiverId, unreadMsgPOList.size());
            // 推送成功，自定义提示
            return Result.custom(ResultCode.SUCCESS, "售前重连成功，已推送" + unreadMsgPOList.size() + "条未读消息");

        } catch (Exception e) {
            logger.error("【售前WebSocket重连异常】sessionId：{}，receiverId：{}，异常信息：{}",
                    sessionId, receiverId, e.getMessage(), e);
            return Result.error("售前重连推送未读消息异常，请稍后重试");
        }
    }

    /**
     * 私有工具：按售前sessionId查找Netty通道
     */
    private Channel findPreSaleChannelBySessionId(String sessionId) {
        Map<String, Channel> channelMap = PreSaleNettyWebSocketServerHandler.PRE_SALE_RECEIVER_CHANNEL_MAP;
        if (CollectionUtils.isEmpty(channelMap)) {
            return null;
        }
        for (Channel channel : channelMap.values()) {
            String bindSessionId = channel.attr(NettyConstant.PRE_SALE_SESSION_ID_KEY).get();
            if (sessionId.equals(bindSessionId)) {
                return channel;
            }
        }
        return null;
    }


    @Override
    public String generatePreSaleSessionId() {
        return "PRE_SESSION_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public String generatePreSaleMsgId(String preSaleSessionId) {
        long timestamp = System.currentTimeMillis();
        String sessionSuffix = preSaleSessionId.substring(preSaleSessionId.length() - 8);
        return "PRE_MSG_" + timestamp + "_" + sessionSuffix;
    }

    @Override
    public Result<Void> savePreSaleChatMessage(PreSaleChatMessageVO vo, String receiverId) {
        try {
            if (!receiverIdService.validateReceiverId(receiverId)) {
                return Result.unauthorized("ReceiverId无效或已过期");
            }
            if (vo == null || vo.getSenderId() == null || vo.getPreSaleSessionId() == null) {
                return Result.error("必要参数不能为空");
            }
            if (vo.getMsgId() == null) {
                vo.setMsgId(generatePreSaleMsgId(vo.getPreSaleSessionId()));
            }
            if (vo.getSenderType() == null) {
                vo.setSenderType(PreSaleChatMessageVO.SENDER_TYPE_USER);
            }
            if (vo.getMsgType() == null) {
                vo.setMsgType(PreSaleChatMessageVO.MSG_TYPE_TEXT);
            }
            if (vo.getIsRead() == null) {
                vo.setIsRead(PreSaleChatMessageVO.IS_READ_NO);
            }
            if (vo.getSendTime() == null) {
                vo.setSendTime(new Date());
            }

            PreSaleChatMessagePO po = new PreSaleChatMessagePO();
            BeanUtils.copyProperties(vo, po);
            int affectedRows = preSaleChatMessageMapper.insertPreSaleChatMessage(po);
            return affectedRows > 0 ? Result.success() : Result.error("保存售前消息失败");
        } catch (Exception e) {
            logger.error("保存售前消息异常", e);
            return Result.error("保存售前消息异常：" + e.getMessage());
        }
    }

    @Override
    public Result<List<PreSaleChatMessageVO>> listByPreSaleSessionId(String preSaleSessionId, String receiverId) {
        try {
            if (!receiverIdService.validateReceiverId(receiverId)) {
                return Result.unauthorized("ReceiverId无效或已过期");
            }
            List<PreSaleChatMessagePO> poList = preSaleChatMessageMapper.listByPreSaleSessionId(preSaleSessionId);
            List<PreSaleChatMessageVO> voList = new ArrayList<>();
            if (!CollectionUtils.isEmpty(poList)) {
                poList.forEach(po -> {
                    PreSaleChatMessageVO vo = new PreSaleChatMessageVO();
                    BeanUtils.copyProperties(po, vo);
                    voList.add(vo);
                });
            }
            return Result.success(voList);
        } catch (Exception e) {
            logger.error("查询售前会话消息异常", e);
            return Result.error("查询售前会话消息异常：" + e.getMessage());
        }
    }

    @Override
    public Result<List<PreSaleChatMessageVO>> listByUserId(String userId, String receiverId) {
        try {
            if (!receiverIdService.validateReceiverId(receiverId)) {
                return Result.unauthorized("ReceiverId无效或已过期");
            }
            List<PreSaleChatMessagePO> poList = preSaleChatMessageMapper.listByUserId(userId);
            List<PreSaleChatMessageVO> voList = new ArrayList<>();
            if (!CollectionUtils.isEmpty(poList)) {
                poList.forEach(po -> {
                    PreSaleChatMessageVO vo = new PreSaleChatMessageVO();
                    BeanUtils.copyProperties(po, vo);
                    voList.add(vo);
                });
            }
            return Result.success(voList);
        } catch (Exception e) {
            logger.error("查询用户售前消息异常", e);
            return Result.error("查询用户售前消息异常：" + e.getMessage());
        }
    }

    @Override
    public Result<Void> cleanExpiredPreSaleChatMessage() {
        try {
            Date expireTime = DateUtil.offsetDay(new Date(), -7);
            int affectedRows = preSaleChatMessageMapper.deleteExpiredPreSaleChatMessage(expireTime);
            logger.info("清理过期售前消息成功，共清理{}条记录", affectedRows);
            return Result.success();
        } catch (Exception e) {
            logger.error("清理过期售前消息异常", e);
            return Result.error("清理过期售前消息异常：" + e.getMessage());
        }
    }
}