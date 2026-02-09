package com.yqrb.mapper;

import com.yqrb.pojo.vo.ChatMessageVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface ChatMessageMapperCustom {


    // ========== 新增：按sessionId+receiverId查询未读消息（is_read=0） ==========
    // ========== 修改点：方法名+参数新增receiverId ==========
    List<ChatMessageVO> listUnreadBySessionIdAndReceiverId(
            @Param("sessionId") String sessionId,
            @Param("receiverId") String receiverId
    );

    // 保存聊天消息
    @Insert("INSERT INTO chat_message (msg_id, sender_id, sender_type, receiver_id, content, " +
            "msg_type, session_id, send_time, is_read, create_time) " +
            "VALUES (#{msgId}, #{senderId}, #{senderType}, #{receiverId}, #{content}, " +
            "#{msgType}, #{sessionId}, #{sendTime}, #{isRead}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertChatMessage(ChatMessageVO chatMessage);

    // 根据会话ID查询消息列表
    // 1. 【修改】无分页查询：重命名方法名，避免与分页方法冲突，同时保留原有功能
    // 原方法名：selectBySessionId → 新方法名：selectAllMessageBySessionId
    @Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} ORDER BY send_time ASC")
    List<ChatMessageVO> selectAllMessageBySessionId(String sessionId);

    // 分页查询会话消息（去掉分页参数，或保留用于PageHelper）
    List<ChatMessageVO> getMessageListBySessionIdWithPage(
            @Param("sessionId") String sessionId,
            @Param("receiverId") String receiverId
    );


    // 根据接收者ID查询会话未读消息
    List<ChatMessageVO> selectUnreadMsgBySessionIdAndReceiverId(String sessionId, String receiverId);

    // 更新消息已读状态（XML实现）
    int updateMsgReadStatus(String msgId);

    // 删除会话所有消息（按sessionId）
    int deleteBySessionId(String sessionId);

    /**
     * 批量更新某个会话的未读消息为已读
     */
    int batchUpdateMsgReadStatusBySessionId(@Param("sessionId") String sessionId, @Param("receiverId") String receiverId);

    /**
     * 根据消息ID（msgId）查询单个聊天消息
     * @param msgId 消息唯一标识
     * @return 聊天消息实体（无匹配数据返回 null）
     */
    ChatMessageVO selectByMsgId(@Param("msgId") String msgId);


    /**
     * 【新增】先查询本次要标记已读的消息条数（N）
     * @param sessionId
     * @param receiverId
     * @return
     */
    int countUnreadMsgBySessionIdAndReceiverId(@Param("sessionId") String sessionId, @Param("receiverId") String receiverId);

    /**
     * 【新增】统计接收者的所有未读消息总数
     * @param receiverId
     * @return
     */
    Long countTotalUnreadMsgByReceiverId(@Param("receiverId") String receiverId);

}