package com.yqrb.service.impl;

import com.yqrb.mapper.ChatMessageMapperCustom;
import com.yqrb.pojo.vo.ChatMessageVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.pojo.vo.WebSocketMsgVO;
import com.yqrb.service.ChatMessageService;
import com.yqrb.service.ReceiverIdService;
import com.yqrb.util.DateUtil;
import com.yqrb.util.UUIDUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
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

        // 3. 构建聊天消息实体
        ChatMessageVO chatMessage = new ChatMessageVO();
        chatMessage.setMsgId(UUIDUtil.generateMsgId());
        chatMessage.setSenderId(webSocketMsg.getUserId());
        chatMessage.setSenderType(ChatMessageVO.SENDER_TYPE_USER);
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
}