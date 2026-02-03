package com.yqrb.service.impl;

import com.yqrb.mapper.ChatMessageMapperCustom;
import com.yqrb.pojo.ChatMessage;
import com.yqrb.pojo.vo.ChatMessageVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.pojo.vo.WebSocketMsgVO;
import com.yqrb.service.ChatMessageService;
import com.yqrb.service.ReceiverIdService;
import com.yqrb.util.DateUtil;
import com.yqrb.util.UUIDUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    @Resource
    private ChatMessageMapperCustom chatMessageMapperCustom;

    @Resource
    private ReceiverIdService receiverIdService;

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
        // 原校验逻辑替换
        String senderType = webSocketMsg.getSenderType();
        // 兼容Java 8的写法：用Arrays.asList替代List.of
        if (!StringUtils.hasText(senderType)
                || !Arrays.asList(ChatMessageVO.SENDER_TYPE_USER, ChatMessageVO.SENDER_TYPE_CS, ChatMessageVO.SENDER_TYPE_SYSTEM).contains(senderType)) {
            senderType = ChatMessageVO.SENDER_TYPE_USER; // 默认值兜底
        }

        // 3. 构建聊天消息实体
        ChatMessageVO chatMessage = new ChatMessageVO();
        chatMessage.setMsgId(UUIDUtil.generateMsgId());
        chatMessage.setSenderId(webSocketMsg.getUserId());
        chatMessage.setSenderType(senderType); // 替换固定值，使用校验后的发送者类型
        chatMessage.setReceiverId(webSocketMsg.getReceiverId());
        chatMessage.setContent(webSocketMsg.getMsgContent());
        chatMessage.setMsgType(webSocketMsg.getMsgType() == null ? ChatMessageVO.MSG_TYPE_TEXT : webSocketMsg.getMsgType());
        chatMessage.setSessionId(webSocketMsg.getSessionId());
        chatMessage.setSendTime(webSocketMsg.getSendTime() == null ? DateUtil.getCurrentDate() : webSocketMsg.getSendTime());
        chatMessage.setIsRead(ChatMessageVO.IS_READ_NO);
        chatMessage.setCreateTime(DateUtil.getCurrentDate());

        // 4. 保存消息到数据库
        int insertResult = chatMessageMapperCustom.insertChatMessage(chatMessage);
        if (insertResult <= 0) {
            return Result.error("发送消息失败");
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
        List<ChatMessageVO> msgList = chatMessageMapperCustom.selectBySessionId(sessionId);

        // 3. 刷新ReceiverId过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);

        return Result.success(msgList);
    }

    @Override
    public Result<List<ChatMessageVO>> getUnreadMessageList(String receiverId) {
        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 2. 查询未读消息
        List<ChatMessageVO> unreadMsgList = chatMessageMapperCustom.selectUnreadMsgByReceiverId(receiverId);

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

        // 3. 可选：校验会话是否存在（可根据需求扩展，查询消息列表判断是否为空）
        List<ChatMessageVO> msgList = chatMessageMapperCustom.selectBySessionId(sessionId);
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

        // 6. 返回成功结果（返回删除的消息条数，也可直接返回Boolean）
        return Result.success(true);
    }
}