package com.yqrb.service.impl;

import cn.hutool.core.date.DateUtil;
import com.yqrb.pojo.vo.ReceiverIdSessionVO;
import com.yqrb.service.ReceiverIdService;
import com.yqrb.util.RedisUtil;
import com.yqrb.util.UUIDUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Service
public class ReceiverIdServiceImpl implements ReceiverIdService {

    @Resource
    private RedisUtil redisUtil;

    @Value("${custom.receiver.session.expire-seconds}")
    private long expireSeconds;

    @Value("${custom.receiver.session.prefix}")
    private String receiverPrefix;

    @Override
    public ReceiverIdSessionVO generateReceiverId(String userId, String userName) {
        // 生成唯一ReceiverId
        String receiverId = UUIDUtil.generateReceiverId(userId);
        Date createTime = new Date();
        Date expireTime = DateUtil.offsetSecond(createTime, (int) expireSeconds);

        // 构建会话对象
        ReceiverIdSessionVO session = new ReceiverIdSessionVO(
                receiverId,
                userId,
                userName,
                createTime,
                expireTime,
                false
        );

        // 存入Redis并设置过期时间
        String redisKey = receiverPrefix + receiverId;
        redisUtil.set(redisKey, session, expireSeconds);

        return session;
    }

    @Override
    public boolean validateReceiverId(String receiverId) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            return false;
        }
        String redisKey = receiverPrefix + receiverId;
        return redisUtil.get(redisKey) != null;
    }

    @Override
    public boolean refreshReceiverIdExpire(String receiverId) {
        if (!validateReceiverId(receiverId)) {
            return false;
        }
        String redisKey = receiverPrefix + receiverId;
        return redisUtil.refreshExpire(redisKey, expireSeconds);
    }

    @Override
    public boolean destroyReceiverId(String receiverId) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            return false;
        }
        String redisKey = receiverPrefix + receiverId;
        return redisUtil.delete(redisKey);
    }

    @Override
    public ReceiverIdSessionVO getReceiverIdSession(String receiverId) {
        if (!validateReceiverId(receiverId)) {
            return null;
        }
        String redisKey = receiverPrefix + receiverId;
        return (ReceiverIdSessionVO) redisUtil.get(redisKey);
    }

    @Override
    public boolean markOnline(String receiverId) {
        ReceiverIdSessionVO session = getReceiverIdSession(receiverId);
        if (session == null) {
            return false;
        }
        session.setIsOnline(true);
        String redisKey = receiverPrefix + receiverId;
        return redisUtil.set(redisKey, session, expireSeconds);
    }

    @Override
    public boolean markOffline(String receiverId) {
        ReceiverIdSessionVO session = getReceiverIdSession(receiverId);
        if (session == null) {
            return false;
        }
        session.setIsOnline(false);
        String redisKey = receiverPrefix + receiverId;
        return redisUtil.set(redisKey, session, expireSeconds);
    }
}