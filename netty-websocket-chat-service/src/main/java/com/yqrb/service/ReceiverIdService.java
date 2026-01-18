package com.yqrb.service;

import com.yqrb.pojo.vo.ReceiverIdSessionVO;

public interface ReceiverIdService {
    // 生成ReceiverId会话
    ReceiverIdSessionVO generateReceiverId(String userId, String userName);

    // 校验ReceiverId有效性
    boolean validateReceiverId(String receiverId);

    // 刷新ReceiverId过期时间
    boolean refreshReceiverIdExpire(String receiverId);

    // 销毁ReceiverId会话
    boolean destroyReceiverId(String receiverId);

    // 获取ReceiverId会话信息
    ReceiverIdSessionVO getReceiverIdSession(String receiverId);

    // 标记用户在线状态
    boolean markOnline(String receiverId);

    // 标记用户离线状态
    boolean markOffline(String receiverId);
}