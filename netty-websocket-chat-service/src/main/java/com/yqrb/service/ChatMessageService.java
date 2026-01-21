package com.yqrb.service;

import com.yqrb.pojo.vo.ChatMessageVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.pojo.vo.WebSocketMsgVO;
import java.util.List;

public interface ChatMessageService {
    // 发送聊天消息（保存到数据库）
    Result<ChatMessageVO> sendMessage(WebSocketMsgVO webSocketMsg, String receiverId);

    // 根据会话ID查询消息列表
    Result<List<ChatMessageVO>> getMessageListBySessionId(String sessionId, String receiverId);

    // 根据接收者ID查询未读消息
    Result<List<ChatMessageVO>> getUnreadMessageList(String receiverId);

    // 标记消息为已读
    Result<Boolean> markMsgAsRead(String msgId, String receiverId);

    // 新增：按sessionId删除会话所有消息
    Result<Boolean> deleteMessageBySessionId(String sessionId, String receiverId);
}