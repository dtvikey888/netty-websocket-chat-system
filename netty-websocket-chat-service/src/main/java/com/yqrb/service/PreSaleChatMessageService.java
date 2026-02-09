package com.yqrb.service;

import com.yqrb.pojo.po.PreSaleChatMessagePO;
import com.yqrb.pojo.vo.PreSaleChatMessageVO;
import com.yqrb.pojo.vo.Result;

import java.util.List;

/**
 * 售前咨询聊天记录Service（完整适配售后逻辑）
 */
public interface PreSaleChatMessageService {

    /**
     * 售前WebSocket重连-推送指定会话+接收方的未读消息
     * @param sessionId 售前会话ID
     * @param receiverId 接收方ID
     * @return 统一响应结果
     */
    Result<Void> wsReconnectPushUnread(String sessionId, String receiverId);

    /**
     * 生成唯一的售前会话ID
     * @return 售前会话ID（格式：PRE_SESSION_+UUID）
     */
    String generatePreSaleSessionId();

    /**
     * 生成唯一的售前消息ID
     * @param preSaleSessionId 售前会话ID
     * @return 售前消息ID（格式：PRE_MSG_+时间戳+会话ID后缀）
     */
    String generatePreSaleMsgId(String preSaleSessionId);

    /**
     * 保存售前消息记录
     * @param preSaleChatMessageVO 售前消息VO
     * @param receiverId 权限校验用ReceiverId
     * @return 保存结果
     */
    Result<Void> savePreSaleChatMessage(PreSaleChatMessageVO preSaleChatMessageVO,String receiverId);

    /**
     * 按售前会话ID查询消息列表（无分页）
     * @param sessionId 售前会话ID
     * @param receiverId 权限校验用ReceiverId
     * @return 消息列表
     */
    Result<List<PreSaleChatMessagePO>> listUnreadBySessionAndReceiver(String sessionId, String receiverId);
    /**
     * 【新增】分页查询售前会话消息
     * @param preSaleSessionId 售前会话ID
     * @param receiverId 权限校验用ReceiverId
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 分页消息列表
     */
    Result<List<PreSaleChatMessageVO>> listByPreSaleSessionIdWithPage(
            String preSaleSessionId, String receiverId, Integer pageNum, Integer pageSize);

    /**
     * 按用户ID查询售前消息列表（供售后客服追溯）
     * @param userId 用户ID
     * @param receiverId 权限校验用ReceiverId
     * @return 消息列表
     */
    Result<List<PreSaleChatMessageVO>> listByUserId(String userId,String receiverId);

    /**
     * 【新增】批量标记售前会话未读消息为已读
     * @param sessionId 售前会话ID
     * @param receiverId 接收方ID
     * @return 操作结果
     */
    Result<Boolean> batchMarkMsgAsReadBySessionId(String sessionId, String receiverId);

    /**
     * 【新增】查询接收方未读消息总数（优先Redis，兜底DB）
     * @param receiverId 接收方ID
     * @return 未读总数
     */
    Result<Long> getUnreadMsgTotalCount(String receiverId);

    /**
     * 【新增】按会话ID删除售前会话所有消息
     * @param sessionId 售前会话ID
     * @param receiverId 权限校验用ReceiverId
     * @return 操作结果
     */
    Result<Boolean> deleteMessageBySessionId(String sessionId, String receiverId);

    /**
     * 清理过期售前消息（默认清理7天前的记录）
     * @return 清理结果
     */
    Result<Void> cleanExpiredPreSaleChatMessage();
}