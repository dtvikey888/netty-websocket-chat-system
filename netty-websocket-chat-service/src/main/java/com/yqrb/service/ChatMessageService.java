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

    // 新增：批量标记某个会话的所有未读消息为已读（核心补充）
    Result<Boolean> batchMarkMsgAsReadBySessionId(String sessionId, String receiverId);

    // 方法查询会话下所有消息，当一个会话有上千条消息时，接口响应慢，前端渲染卡顿，实现分页查询是最优解。
    Result<List<ChatMessageVO>> getMessageListBySessionId(String sessionId, Integer pageNum, Integer pageSize, String receiverId);
}