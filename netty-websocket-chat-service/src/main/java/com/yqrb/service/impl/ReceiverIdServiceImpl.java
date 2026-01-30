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
        String receiverId;
        // ========== 适配乐音清扬用户：根据userId格式识别，生成固定ID（生成规则不变） ==========
        if (userId.startsWith("LYQY_")) {
            String[] userIdParts = userId.split("_");
            if (userIdParts.length >= 3) {
                String userType = userIdParts[1]; // USER/ADMIN/CS
                String appId = userIdParts[2];    // 用户专属appId
                receiverId = UUIDUtil.generateLyqyFixedReceiverId(userType, appId);
            } else {
                // 格式不合法时，降级为随机ID
                receiverId = UUIDUtil.generateReceiverId(userId);
            }
        } else {
            // 非乐音清扬用户，仍用原有随机规则
            receiverId = UUIDUtil.generateReceiverId(userId);
        }

        // 原有逻辑不变：构建会话对象并存入Redis（固定ID也会存入Redis，为后续校验做准备）
        Date createTime = new Date();
        Date expireTime = DateUtil.offsetSecond(createTime, (int) expireSeconds);

        ReceiverIdSessionVO session = new ReceiverIdSessionVO(
                receiverId,
                userId,
                userName,
                createTime,
                expireTime,
                false
        );

        String redisKey = receiverPrefix + receiverId;
        redisUtil.set(redisKey, session, expireSeconds);

        return session;
    }

    // ========== 核心修改1：validateReceiverId 移除固定ID直接放行，统一Redis校验 ==========
    @Override
    public boolean validateReceiverId(String receiverId) {
        // 1. 先做非空校验（所有ID都需要满足）
        if (receiverId == null || receiverId.trim().isEmpty()) {
            return false;
        }
        // 2. 拼接Redis Key（固定ID和普通ID格式一致）
        String redisKey = receiverPrefix + receiverId;
        // 3. 查询Redis，存在且未过期则合法（无任何特殊放行，统一校验）
        return redisUtil.get(redisKey) != null;
    }

    // ========== 核心修改2：refreshReceiverIdExpire 移除固定ID直接返回，统一刷新逻辑 ==========
    @Override
    public boolean refreshReceiverIdExpire(String receiverId) {
        // 1. 先校验ID合法性（固定ID也会走Redis校验）
        if (!validateReceiverId(receiverId)) {
            return false;
        }
        // 2. 拼接Redis Key，刷新过期时间（所有合法ID统一处理）
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

    // ========== 核心修改3：markOnline 移除固定ID直接放行，统一Redis校验+状态更新 ==========
    @Override
    public boolean markOnline(String receiverId) {
        // 1. 先通过Redis校验获取会话（固定ID也需要校验）
        ReceiverIdSessionVO session = getReceiverIdSession(receiverId);
        if (session == null) {
            return false;
        }
        // 2. 更新在线状态，重新存入Redis（所有合法ID统一处理）
        session.setIsOnline(true);
        String redisKey = receiverPrefix + receiverId;
        return redisUtil.set(redisKey, session, expireSeconds);
    }

    // ========== 核心修改4：markOffline 移除固定ID直接放行，统一Redis校验+状态更新 ==========
    @Override
    public boolean markOffline(String receiverId) {
        // 1. 先通过Redis校验获取会话（固定ID也需要校验）
        ReceiverIdSessionVO session = getReceiverIdSession(receiverId);
        if (session == null) {
            return false;
        }
        // 2. 更新离线状态，重新存入Redis（所有合法ID统一处理）
        session.setIsOnline(false);
        String redisKey = receiverPrefix + receiverId;
        return redisUtil.set(redisKey, session, expireSeconds);
    }
}