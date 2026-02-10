package com.yqrb.service.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 售前未读消息数 Redis 缓存服务（独立隔离，与售后解耦）
 */
@Service
public class PreSaleRedisUnreadMsgCacheService {
    private static final Logger log = LoggerFactory.getLogger(PreSaleRedisUnreadMsgCacheService.class);

    // 核心区别：Key 前缀增加 pre-sale 标识，与售后（chat:unread:count:）做隔离
    private static final String PRE_SALE_UNREAD_MSG_KEY_PREFIX = "pre-sale:chat:unread:count:";
    // 售前缓存过期时间：可独立配置，无需和售后保持一致
    private static final long CACHE_EXPIRE_DAYS = 7;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 售前未读消息数 +1（原子操作，高并发安全）
     */
    public Long incrUnreadMsgCount(String receiverId) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            log.warn("【售前 Redis 缓存】接收者 ID 为空，无法递增未读消息数");
            return 0L;
        }
        String redisKey = buildRedisKey(receiverId);
        try {
            Long newCount = stringRedisTemplate.opsForValue().increment(redisKey);
            if (newCount != null && newCount == 1) {
                stringRedisTemplate.expire(redisKey, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            }
            return newCount == null ? 0L : newCount;
        } catch (Exception e) {
            log.error("【售前 Redis 缓存】递增未读消息数失败，receiverId：{}", receiverId, e);
            return 0L;
        }
    }

    /**
     * 售前未读消息数递减（避免负数）
     */
    public Long decrUnreadMsgCount(String receiverId, Integer decrementNum) {
        if (receiverId == null || receiverId.trim().isEmpty() || decrementNum <= 0) {
            log.warn("【售前 Redis 缓存】参数无效，无法递减未读消息数");
            return 0L;
        }
        String redisKey = buildRedisKey(receiverId);
        try {
            String currentCountStr = stringRedisTemplate.opsForValue().get(redisKey);
            if (currentCountStr == null) {
                return 0L;
            }
            Long currentCount = Long.parseLong(currentCountStr);

            if (currentCount <= decrementNum) {
                stringRedisTemplate.delete(redisKey);
                return 0L;
            }

            Long newCount = stringRedisTemplate.opsForValue().decrement(redisKey, decrementNum);
            return newCount == null ? 0L : Math.max(newCount, 0L);
        } catch (Exception e) {
            log.error("【售前 Redis 缓存】递减未读消息数失败，receiverId：{}", receiverId, e);
            return 0L;
        }
    }

    /**
     * 查询售前未读消息数（先缓存后DB，兜底更新）
     */
    public Long getUnreadMsgCount(String receiverId, Supplier<Long> dbQueryFunc) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            return 0L;
        }
        String redisKey = buildRedisKey(receiverId);

        // 1. 先查缓存
        try {
            String cacheCountStr = stringRedisTemplate.opsForValue().get(redisKey);
            if (cacheCountStr != null) {
                return Long.parseLong(cacheCountStr);
            }
        } catch (Exception e) {
            log.error("【售前 Redis 缓存】查询未读消息数失败，降级查询 DB，receiverId：{}", receiverId, e);
            return dbQueryFunc.get();
        }

        // 2. 缓存未命中，查DB并更新缓存
        Long dbCount = dbQueryFunc.get();
        try {
            if (dbCount > 0) {
                stringRedisTemplate.opsForValue().set(redisKey, dbCount.toString(), CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            } else {
                stringRedisTemplate.delete(redisKey);
            }
        } catch (Exception e) {
            log.error("【售前 Redis 缓存】更新未读消息数缓存失败，receiverId：{}", receiverId, e);
        }

        return dbCount;
    }

    /**
     * 清零售前未读消息数
     */
    public void clearUnreadMsgCount(String receiverId) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            return;
        }
        String redisKey = buildRedisKey(receiverId);
        try {
            stringRedisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.error("【售前 Redis 缓存】清零未读消息数失败，receiverId：{}", receiverId, e);
        }
    }

    /**
     * 构建售前专属 Redis Key（核心隔离点）
     */
    private String buildRedisKey(String receiverId) {
        return PRE_SALE_UNREAD_MSG_KEY_PREFIX + receiverId;
    }
}