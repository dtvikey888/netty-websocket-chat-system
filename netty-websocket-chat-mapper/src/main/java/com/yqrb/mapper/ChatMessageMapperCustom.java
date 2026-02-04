package com.yqrb.mapper;

import com.yqrb.pojo.vo.ChatMessageVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface ChatMessageMapperCustom {

    // 保存聊天消息
    @Insert("INSERT INTO chat_message (msg_id, sender_id, sender_type, receiver_id, content, " +
            "msg_type, session_id, send_time, is_read, create_time) " +
            "VALUES (#{msgId}, #{senderId}, #{senderType}, #{receiverId}, #{content}, " +
            "#{msgType}, #{sessionId}, #{sendTime}, #{isRead}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertChatMessage(ChatMessageVO chatMessage);

    // 根据会话ID查询消息列表
    @Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} ORDER BY send_time ASC")
    List<ChatMessageVO> selectBySessionId(String sessionId);

    // 根据接收者ID查询未读消息
    List<ChatMessageVO> selectUnreadMsgByReceiverId(String receiverId);

    // 更新消息已读状态（XML实现）
    int updateMsgReadStatus(String msgId);

    // 删除会话所有消息（按sessionId）
    int deleteBySessionId(String sessionId);

    /**
     * 批量更新某个会话的未读消息为已读
     */
    int batchUpdateMsgReadStatusBySessionId(@Param("sessionId") String sessionId, @Param("receiverId") String receiverId);
}