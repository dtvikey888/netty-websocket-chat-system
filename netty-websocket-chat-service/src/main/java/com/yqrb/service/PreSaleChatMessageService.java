package com.yqrb.service;

import com.yqrb.pojo.vo.PreSaleChatMessageVO;
import com.yqrb.pojo.vo.Result;


import java.util.List;

/**
 * 售前咨询聊天记录Service
 */
public interface PreSaleChatMessageService {
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
     * @return 保存结果
     */
    Result<Void> savePreSaleChatMessage(PreSaleChatMessageVO preSaleChatMessageVO,String receiverId);

    /**
     * 按售前会话ID查询消息列表
     * @param preSaleSessionId 售前会话ID
     * @return 消息列表
     */
    Result<List<PreSaleChatMessageVO>> listByPreSaleSessionId(String preSaleSessionId,String receiverId);

    /**
     * 按用户ID查询售前消息列表（供售后客服追溯）
     * @param userId 用户ID
     * @return 消息列表
     */
    Result<List<PreSaleChatMessageVO>> listByUserId(String userId,String receiverId);

    /**
     * 清理过期售前消息（默认清理7天前的记录）
     * @return 清理结果
     */
    Result<Void> cleanExpiredPreSaleChatMessage();
}