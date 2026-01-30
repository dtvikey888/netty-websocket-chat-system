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
        // ========== 适配乐音清扬用户：根据userId格式识别，生成固定ID ==========
        // 乐音清扬userId格式：LYQY_<userType>_<appId>（如LYQY_USER_001、LYQY_ADMIN_abc123）
        if (userId.startsWith("LYQY_")) {
            String[] userIdParts = userId.split("_");
            if (userIdParts.length >= 3) {
                String userType = userIdParts[1]; // USER/ADMIN/CS
                String appId = userIdParts[2];    // 用户专属appId
                // 调用工具类生成固定ReceiverId
                receiverId = UUIDUtil.generateLyqyFixedReceiverId(userType, appId);
            } else {
                // 格式不合法时，降级为随机ID
                receiverId = UUIDUtil.generateReceiverId(userId);
            }
        } else {
            // 非乐音清扬用户，仍用原有随机规则
            receiverId = UUIDUtil.generateReceiverId(userId);
        }

        // 原有逻辑不变：构建会话对象并存入Redis
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

    // ========== 其他方法（validateReceiverId/refreshExpire等）保持不变 ==========
    @Override
    public boolean validateReceiverId(String receiverId) {
        // 乐音清扬固定ID直接放行（测试/正式都兼容）
        if (receiverId != null && receiverId.startsWith("R_FIXED_0000_LYQY_")) {
            return true;
        }
        if (receiverId == null || receiverId.trim().isEmpty()) {
            return false;
        }
        String redisKey = receiverPrefix + receiverId;
        return redisUtil.get(redisKey) != null;
    }

    @Override
    public boolean refreshReceiverIdExpire(String receiverId) {
        // 第一步：优先判断是否是乐音清扬固定ID，直接返回true，不执行后续Redis操作
        if (receiverId != null && receiverId.startsWith("R_FIXED_0000_LYQY_")) {
            String redisKey = receiverPrefix + receiverId;
            // 调用RedisUtil刷新过期时间，返回真实执行结果
            return redisUtil.refreshExpire(redisKey, expireSeconds);
        }
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
        if (receiverId != null && receiverId.startsWith("R_FIXED_0000_LYQY_")) {
            return true;
        }
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
        if (receiverId != null && receiverId.startsWith("R_FIXED_0000_LYQY_")) {
            return true;
        }
        ReceiverIdSessionVO session = getReceiverIdSession(receiverId);
        if (session == null) {
            return false;
        }
        session.setIsOnline(false);
        String redisKey = receiverPrefix + receiverId;
        return redisUtil.set(redisKey, session, expireSeconds);
    }
}