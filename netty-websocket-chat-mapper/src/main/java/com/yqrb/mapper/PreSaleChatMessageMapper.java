package com.yqrb.mapper;

import com.yqrb.pojo.po.PreSaleChatMessagePO;
import com.yqrb.pojo.vo.PreSaleChatMessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 售前咨询聊天记录Mapper（补充未读统计、批量标记已读、删除会话消息）
 */
@Mapper
public interface PreSaleChatMessageMapper {

    /**
     * 按sessionId + receiverId 查询售前未读消息（is_read=0）
     * @param sessionId 售前会话ID
     * @param receiverId 接收方ID
     * @return 未读消息列表
     */
    List<PreSaleChatMessagePO> listUnreadBySessionIdAndReceiverId(
            @Param("sessionId") String sessionId,
            @Param("receiverId") String receiverId
    );

    /**
     * 插入一条售前消息记录
     * @param preSaleChatMessagePO 售前消息PO
     * @return 受影响行数
     */
    int insertPreSaleChatMessage(PreSaleChatMessagePO preSaleChatMessagePO);

    /**
     * 按售前会话ID查询该会话的所有消息（按发送时间正序）
     * @param preSaleSessionId 售前会话ID
     * @return 售前消息列表
     */
    List<PreSaleChatMessagePO> listByPreSaleSessionId(@Param("preSaleSessionId") String preSaleSessionId);

    /**
     * 按用户ID查询该用户的所有售前消息（按发送时间倒序）
     * @param userId 用户ID
     * @return 售前消息列表
     */
    List<PreSaleChatMessagePO> listByUserId(@Param("userId") String userId);

    /**
     * 【新增】统计会话下接收者的未读消息条数
     * @param sessionId 售前会话ID
     * @param receiverId 接收方ID
     * @return 未读消息数
     */
    int countUnreadMsgBySessionIdAndReceiverId(
            @Param("sessionId") String sessionId,
            @Param("receiverId") String receiverId
    );

    /**
     * 【新增】统计接收者的所有未读消息总数
     * @param receiverId 接收方ID
     * @return 未读总数
     */
    Long countTotalUnreadMsgByReceiverId(@Param("receiverId") String receiverId);

    /**
     * 【新增】批量更新会话下未读消息为已读
     * @param sessionId 售前会话ID
     * @param receiverId 接收方ID
     * @return 受影响行数
     */
    int batchUpdateMsgReadStatusBySessionId(
            @Param("sessionId") String sessionId,
            @Param("receiverId") String receiverId
    );

    /**
     * 【新增】按会话ID删除所有消息
     * @param sessionId 售前会话ID
     * @return 受影响行数
     */
    int deleteBySessionId(@Param("sessionId") String sessionId);

    /**
     * 按时间清理过期售前消息
     * @param expireTime 过期时间（早于该时间的记录将被删除）
     * @return 受影响行数
     */
    int deleteExpiredPreSaleChatMessage(@Param("expireTime") Date expireTime);
}