package com.yqrb.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yqrb.mapper.ChatMessageMapperCustom;
import com.yqrb.netty.NettyWebSocketServerHandler;
import com.yqrb.netty.NettyWebSocketUtil;
import com.yqrb.netty.constant.NettyConstant;
import com.yqrb.pojo.vo.ChatMessageVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.pojo.vo.ResultCode;
import com.yqrb.pojo.vo.WebSocketMsgVO;
import com.yqrb.service.ChatMessageService;
import com.yqrb.service.ReceiverIdService;
import com.yqrb.service.cache.RedisUnreadMsgCacheService;
import com.yqrb.util.DateUtil;
import com.yqrb.util.UUIDUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageServiceImpl.class);

    @Resource
    private ChatMessageMapperCustom chatMessageMapperCustom;

    @Resource
    private ReceiverIdService receiverIdService;

    // 统一注入：仅保留 RedisUnreadMsgCacheService（移除冗余的 UnreadMsgCacheService 引用）
    @Resource
    private RedisUnreadMsgCacheService redisUnreadMsgCacheService;

    // ========== 修改点：方法参数新增receiverId，核心逻辑适配 ==========
    @Override
    public Result<Void> wsReconnectPushUnread(String sessionId, String receiverId) {
        // 1. 参数校验：sessionId + receiverId 均非空
        if (!StringUtils.hasText(sessionId)) {
            return Result.paramError("sessionId不能为空");
        }
        if (!StringUtils.hasText(receiverId)) { // 新增：校验receiverId
            return Result.paramError("receiverId不能为空");
        }

        try {
            // 2. 按sessionId + receiverId 查询未读消息（核心修正）
            List<ChatMessageVO> unreadMsgPOList = chatMessageMapperCustom.listUnreadBySessionIdAndReceiverId(sessionId, receiverId);
            if (CollectionUtils.isEmpty(unreadMsgPOList)) {
                // 修正点1：用custom方法返回Void类型+自定义msg，避免泛型冲突
                return Result.custom(ResultCode.SUCCESS, "重连成功，当前会话无未读消息");
            }

            // 3. 查找通道：也可通过receiverId直接找（更高效，替代原有遍历）
            Channel targetChannel = NettyWebSocketServerHandler.RECEIVER_CHANNEL_MAP.get(receiverId);
            // 兜底：如果receiverId找不到，再用sessionId遍历（兼容原有逻辑）
            if (targetChannel == null) {
                targetChannel = findChannelBySessionId(sessionId);
            }
            if (targetChannel == null || !targetChannel.isActive() || !targetChannel.isWritable()) {
                // 修正点2：error方法返回Void类型，msg自定义，无数据
                return Result.error("重连成功，但会话通道未在线，未读消息将在通道上线后自动推送");
            }

            // 4. 批量推送（逻辑不变，格式兼容）
            for (ChatMessageVO po : unreadMsgPOList) {
                WebSocketMsgVO wsMsg = new WebSocketMsgVO();
                wsMsg.setSessionId(po.getSessionId());
                wsMsg.setReceiverId(po.getReceiverId()); // 确保是当前接收方
                wsMsg.setUserId(po.getSenderId());
                wsMsg.setMsgContent(po.getContent());
                wsMsg.setMsgType(po.getMsgType());
                wsMsg.setSendTime(po.getSendTime());
                wsMsg.setSenderType(po.getSenderType());

                targetChannel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(wsMsg)));
            }

            log.info("【WebSocket重连】sessionId：{}，receiverId：{}，成功推送{}条未读消息",
                    sessionId, receiverId, unreadMsgPOList.size());
            // 修正点3：用custom方法返回Void类型+自定义成功msg
            return Result.custom(ResultCode.SUCCESS, "重连成功，已推送" + unreadMsgPOList.size() + "条未读消息");

        } catch (Exception e) {
            log.error("【WebSocket重连异常】sessionId：{}，receiverId：{}，异常信息：{}",
                    sessionId, receiverId, e.getMessage(), e);
            return Result.error("重连推送未读消息异常，请稍后重试");
        }
    }

    // 原有工具方法保留（兜底用）
    private Channel findChannelBySessionId(String sessionId) {
        Map<String, Channel> channelMap = NettyWebSocketServerHandler.RECEIVER_CHANNEL_MAP;
        if (CollectionUtils.isEmpty(channelMap)) {
            return null;
        }
        for (Channel channel : channelMap.values()) {
            String bindSessionId = channel.attr(NettyConstant.SESSION_ID_KEY).get();
            if (sessionId.equals(bindSessionId)) {
                return channel;
            }
        }
        return null;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<ChatMessageVO> sendMessage(WebSocketMsgVO webSocketMsg, String receiverId) {
        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 2. 校验消息参数
        if (webSocketMsg.getSessionId() == null || webSocketMsg.getMsgContent() == null) {
            return Result.paramError("会话ID和消息内容不能为空");
        }

        // 新增：校验并处理发送者类型
        String senderType = webSocketMsg.getSenderType();
        if (!StringUtils.hasText(senderType)
                || !Arrays.asList(ChatMessageVO.SENDER_TYPE_USER, ChatMessageVO.SENDER_TYPE_CS, ChatMessageVO.SENDER_TYPE_SYSTEM).contains(senderType)) {
            senderType = ChatMessageVO.SENDER_TYPE_USER; // 默认值兜底
        }

        // 3. 构建聊天消息实体
        ChatMessageVO chatMessage = new ChatMessageVO();
        String msgId = UUIDUtil.generateMsgId();
        chatMessage.setMsgId(msgId);
        chatMessage.setSenderId(webSocketMsg.getUserId());
        chatMessage.setSenderType(senderType);
        chatMessage.setReceiverId(webSocketMsg.getReceiverId());
        chatMessage.setContent(webSocketMsg.getMsgContent());
        chatMessage.setMsgType(webSocketMsg.getMsgType() == null ? ChatMessageVO.MSG_TYPE_TEXT : webSocketMsg.getMsgType());
        chatMessage.setSessionId(webSocketMsg.getSessionId());
        chatMessage.setSendTime(webSocketMsg.getSendTime() == null ? DateUtil.getCurrentDate() : webSocketMsg.getSendTime());
        chatMessage.setIsRead(ChatMessageVO.IS_READ_NO);
        chatMessage.setCreateTime(DateUtil.getCurrentDate());

        // 4. 保存消息到数据库（捕获唯一索引冲突异常，实现幂等）
        try {
            int insertResult = chatMessageMapperCustom.insertChatMessage(chatMessage);
            if (insertResult <= 0) {
                return Result.error("发送消息失败");
            }

            // ========== 【修改1：调整缓存更新顺序】入库成功后，再更新Redis缓存 ==========
            // 最后：Redis 未读消息数 +1（原子操作，高并发安全）
            if (StringUtils.hasText(webSocketMsg.getReceiverId())) {
                redisUnreadMsgCacheService.incrUnreadMsgCount(webSocketMsg.getReceiverId());
            }
        } catch (Exception e) {
            // 捕获唯一索引冲突异常（msg_id 重复）
            if (e.getMessage().contains("uk_msg_id")) {
                log.info("【消息发送幂等校验】消息已存在，msgId：{}", msgId);
                ChatMessageVO existMsg = chatMessageMapperCustom.selectByMsgId(msgId);
                if (existMsg == null) {
                    log.warn("【消息发送幂等校验】消息ID存在冲突，但未查询到对应消息，msgId：{}", msgId);
                    return Result.success(chatMessage);
                }
                return Result.success(existMsg);
            }
            throw e; // 其他异常正常抛出，触发事务回滚
        }

        // 5. 刷新ReceiverId过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);

        // 6. 返回消息详情
        return Result.success(chatMessage);
    }

    @Override
    public Result<List<ChatMessageVO>> getMessageListBySessionId(String sessionId, String receiverId) {
        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 2. 查询消息列表
        List<ChatMessageVO> msgList = chatMessageMapperCustom.selectAllMessageBySessionId(sessionId);

        // 3. 刷新ReceiverId过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);

        return Result.success(msgList);
    }

    /**
     * 【新增】分页查询会话消息（调用Mapper的selectBySessionId）
     */
    @Override
    public Result<List<ChatMessageVO>> getMessageListBySessionIdWithPage(
            String sessionId, String receiverId, Integer pageNum, Integer pageSize) {
        // 1. 校验 ReceiverId 有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 2. 分页参数校验与兜底
        pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        pageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);

        // 3. 开启分页（关键：紧邻后续的 MyBatis 查询）
        PageHelper.startPage(pageNum, pageSize);

        String result;
        if (receiverId.startsWith("R_FIXED_0000_")) {
            result = receiverId.substring("R_FIXED_0000_".length());
        } else {
            result = receiverId;
        }
        // 4. 调用 Mapper 查询（PageHelper 自动拼接分页语句）
        List<ChatMessageVO> messageList = chatMessageMapperCustom.getMessageListBySessionIdWithPage(sessionId, result);

        // 5. 封装分页结果
        PageInfo<ChatMessageVO> pageInfo = new PageInfo<>(messageList);
        log.info("【分页查询会话消息】会话ID：{}，接收者：{}，当前页：{}，每页条数：{}，总条数：{}，总页数：{}",
                sessionId, receiverId, pageInfo.getPageNum(), pageInfo.getPageSize(),
                pageInfo.getTotal(), pageInfo.getPages());

        // 6. 刷新 ReceiverId 过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);

        // 7. 返回结果
        return Result.success(messageList);
    }

    @Override
    public Result<List<ChatMessageVO>> getUnreadMessageListBySessionId(String sessionId,String receiverId) {
        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        String result;
        if (receiverId.startsWith("R_FIXED_0000_")) {
            result = receiverId.substring("R_FIXED_0000_".length());
        } else {
            result = receiverId;
        }

        // 2. 查询会话未读消息
        List<ChatMessageVO> unreadMsgList = chatMessageMapperCustom.selectUnreadMsgBySessionIdAndReceiverId(sessionId, result);

        // 3. 刷新ReceiverId过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);

        return Result.success(unreadMsgList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> markMsgAsRead(String msgId, String receiverId) {
        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 2. 标记消息为已读
        int updateResult = chatMessageMapperCustom.updateMsgReadStatus(msgId);
        if (updateResult <= 0) {
            return Result.error("标记消息已读失败");
        }

        // 3. 刷新ReceiverId过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);

        return Result.success(true);
    }

    // 新增：按sessionId删除会话所有消息
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> deleteMessageBySessionId(String sessionId, String receiverId) {
        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期，无删除消息权限");
        }

        // 2. 校验sessionId参数完整性
        if (!StringUtils.hasText(sessionId)) {
            return Result.paramError("会话ID（sessionId）不能为空");
        }

        // 3. 可选：校验会话是否存在
        List<ChatMessageVO> msgList = chatMessageMapperCustom.selectAllMessageBySessionId(sessionId);
        if (msgList == null || msgList.isEmpty()) {
            return Result.error("该会话无消息记录，无需删除（sessionId：" + sessionId + "）");
        }

        // 4. 调用Mapper执行批量删除操作
        int deleteResult = chatMessageMapperCustom.deleteBySessionId(sessionId);
        if (deleteResult <= 0) {
            return Result.error("删除会话消息失败，请重试");
        }

        // 5. 刷新ReceiverId过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);

        // 6. 返回成功结果
        return Result.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> batchMarkMsgAsReadBySessionId(String sessionId, String receiverId) {
        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 2. 校验sessionId参数
        if (!StringUtils.hasText(sessionId)) {
            return Result.paramError("会话ID（sessionId）不能为空");
        }

        String result;
        if (receiverId.startsWith("R_FIXED_0000_")) {
            result = receiverId.substring("R_FIXED_0000_".length());
        } else {
            result = receiverId;
        }

        // ========== 【修改2：查询未读数传入截取后的 result，保证查询准确】 ==========
        int unreadCount = chatMessageMapperCustom.countUnreadMsgBySessionIdAndReceiverId(sessionId, result);
        if (unreadCount <= 0) {
            return Result.success(true);
        }

        // 3. 调用Mapper批量更新未读消息为已读
        int updateResult = chatMessageMapperCustom.batchUpdateMsgReadStatusBySessionId(sessionId, result);
        if (updateResult <= 0) {
            log.info("【批量标记已读】无未读消息需要更新，会话ID：{}，接收者：{}", sessionId, result);
            return Result.success(true);
        }

        // ========== 【修改3：新增Redis缓存递减操作，保证缓存与DB一致】 ==========
        redisUnreadMsgCacheService.decrUnreadMsgCount(result, unreadCount);

        // 4. 刷新ReceiverId过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);

        log.info("【批量标记已读成功】会话ID：{}，接收者：{}，共标记{}条消息为已读",
                sessionId, receiverId, updateResult);
        return Result.success(true);
    }

    @Override
    public Result<Long> getUnreadMsgTotalCount(String receiverId) {
        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        String result;
        if (receiverId.startsWith("R_FIXED_0000_")) {
            result = receiverId.substring("R_FIXED_0000_".length());
        } else {
            result = receiverId;
        }

        // ========== 【修改4：统一缓存服务引用，避免空指针】 ==========
        long unreadTotal = redisUnreadMsgCacheService.getUnreadMsgCount(result, () -> {
            // 回调函数：数据库查询未读消息总数
            return chatMessageMapperCustom.countTotalUnreadMsgByReceiverId(result);
        });

        // 2. 返回结果
        return Result.success(unreadTotal);
    }



}