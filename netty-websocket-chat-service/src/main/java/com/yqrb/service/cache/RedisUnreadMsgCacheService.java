package com.yqrb.service.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 未读消息数 Redis 缓存服务（解耦设计：专门封装 Redis 操作，业务层无感知）
 */
@Service
public class RedisUnreadMsgCacheService {
    private static final Logger log = LoggerFactory.getLogger(RedisUnreadMsgCacheService.class);

    // Redis Key 前缀：规范命名，避免 Key 冲突
    private static final String UNREAD_MSG_KEY_PREFIX = "chat:unread:count:";
    // 缓存过期时间：7 天（符合你要求，避免无效缓存占用内存）
    private static final long CACHE_EXPIRE_DAYS = 7;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 【原子性操作】未读消息数 +1（高并发安全，使用 Redis INCR 原子操作）
     * @param receiverId 接收者 ID
     * @return 递增后的未读消息数
     */
    public Long incrUnreadMsgCount(String receiverId) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            log.warn("【Redis 缓存】接收者 ID 为空，无法递增未读消息数");
            return 0L;
        }
        String redisKey = buildRedisKey(receiverId);
        try {
            // 核心：INCR 是 Redis 原子操作，高并发下不会计数错乱（符合你要求，避免手动查询+修改+保存的并发问题）
            Long newCount = stringRedisTemplate.opsForValue().increment(redisKey);
            // 设置过期时间：仅当 Key 是新创建时（计数=1），设置 7 天过期，避免重复设置过期时间
            if (newCount != null && newCount == 1) {
                stringRedisTemplate.expire(redisKey, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            }
            return newCount == null ? 0L : newCount;
        } catch (Exception e) {
            log.error("【Redis 缓存】递增未读消息数失败，receiverId：{}", receiverId, e);
            // 缓存更新失败，不影响业务（最终一致性：后续查询会从 DB 加载并更新缓存）
            return 0L;
        }
    }

    /**
     * 【避免负数】未读消息数递减（或清零），防止出现负数
     * @param receiverId 接收者 ID
     * @param decrementNum 要减少的数量（批量已读时使用）
     * @return 递减后的未读消息数（若<=0，返回 0 并删除 Key）
     */
    public Long decrUnreadMsgCount(String receiverId, Integer decrementNum) {
        if (receiverId == null || receiverId.trim().isEmpty() || decrementNum <= 0) {
            log.warn("【Redis 缓存】参数无效，无法递减未读消息数");
            return 0L;
        }
        String redisKey = buildRedisKey(receiverId);
        try {
            // 步骤 1：先查询当前缓存数量（避免直接递减出现负数）
            String currentCountStr = stringRedisTemplate.opsForValue().get(redisKey);
            if (currentCountStr == null) {
                return 0L; // 缓存不存在，无需递减
            }
            Long currentCount = Long.parseLong(currentCountStr);

            // 步骤 2：判断是否需要清零（当前数量 <= 要减少的数量，直接删除 Key 避免负数）
            if (currentCount <= decrementNum) {
                stringRedisTemplate.delete(redisKey);
                return 0L;
            }

            // 步骤 3：原子递减（仅当当前数量 > 要减少的数量时，执行 DECRBY 原子操作）
            Long newCount = stringRedisTemplate.opsForValue().decrement(redisKey, decrementNum);
            return newCount == null ? 0L : Math.max(newCount, 0L);
        } catch (Exception e) {
            log.error("【Redis 缓存】递减未读消息数失败，receiverId：{}", receiverId, e);
            // 缓存更新失败，不影响业务（最终一致性：后续查询会从 DB 加载并更新缓存）
            return 0L;
        }
    }

    /**
     * 查询未读消息数（先查缓存，再查 DB 兜底，符合最终一致性）
     * @param receiverId 接收者 ID
     * @param dbQueryFunc 数据库查询函数（兜底使用，解耦 DB 查询逻辑）
     * @return 未读消息总数
     */
    public Long getUnreadMsgCount(String receiverId, java.util.function.Supplier<Long> dbQueryFunc) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            return 0L;
        }
        String redisKey = buildRedisKey(receiverId);

        // 步骤 1：先查 Redis 缓存（高性能，适合小红点展示）
        try {
            String cacheCountStr = stringRedisTemplate.opsForValue().get(redisKey);
            if (cacheCountStr != null) {
                return Long.parseLong(cacheCountStr);
            }
        } catch (Exception e) {
            log.error("【Redis 缓存】查询未读消息数失败，降级查询 DB，receiverId：{}", receiverId, e);
            // 缓存查询失败，直接降级查询 DB（不影响业务，最终一致性）
            return dbQueryFunc.get();
        }

        // 步骤 2：缓存未命中，查询 DB 并更新缓存（最终一致性，同时设置过期时间）
        Long dbCount = dbQueryFunc.get();
        try {
            if (dbCount > 0) {
                stringRedisTemplate.opsForValue().set(redisKey, dbCount.toString(), CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            } else {
                stringRedisTemplate.delete(redisKey); // DB 数量为 0，删除缓存避免脏数据
            }
        } catch (Exception e) {
            log.error("【Redis 缓存】更新未读消息数缓存失败，receiverId：{}", receiverId, e);
            // 缓存更新失败，不影响业务（返回 DB 查询结果即可，后续查询会重试）
        }

        return dbCount;
    }

    /**
     * 清零未读消息数（删除 Redis Key，高效清零）
     * @param receiverId 接收者 ID
     */
    public void clearUnreadMsgCount(String receiverId) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            return;
        }
        String redisKey = buildRedisKey(receiverId);
        try {
            stringRedisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.error("【Redis 缓存】清零未读消息数失败，receiverId：{}", receiverId, e);
        }
    }

    /**
     * 构建 Redis Key（规范命名，避免 Key 冲突）
     * @param receiverId 接收者 ID
     * @return 完整的 Redis Key
     */
    private String buildRedisKey(String receiverId) {
        return UNREAD_MSG_KEY_PREFIX + receiverId;
    }
}