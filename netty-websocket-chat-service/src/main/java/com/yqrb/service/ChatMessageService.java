package com.yqrb.service;

import com.yqrb.pojo.vo.ChatMessageVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.pojo.vo.WebSocketMsgVO;
import java.util.List;

public interface ChatMessageService {
    // 发送聊天消息（保存到数据库）
    Result<ChatMessageVO> sendMessage(WebSocketMsgVO webSocketMsg, String receiverId);

    //  无分页：根据会话ID查询消息列表（原有方法）
    Result<List<ChatMessageVO>> getMessageListBySessionId(String sessionId, String receiverId);

    // 【新增】分页：根据会话ID查询消息列表（调用Mapper的selectBySessionId）
    Result<List<ChatMessageVO>> getMessageListBySessionIdWithPage(String sessionId, String receiverId, Integer pageNum, Integer pageSize);

    // 根据接收者ID查询未读消息
    Result<List<ChatMessageVO>> getUnreadMessageList(String receiverId);

    // 标记消息为已读
    Result<Boolean> markMsgAsRead(String msgId, String receiverId);

    // 新增：按sessionId删除会话所有消息
    Result<Boolean> deleteMessageBySessionId(String sessionId, String receiverId);

    // 新增：批量标记某个会话的所有未读消息为已读（核心补充）
    Result<Boolean> batchMarkMsgAsReadBySessionId(String sessionId, String receiverId);

}