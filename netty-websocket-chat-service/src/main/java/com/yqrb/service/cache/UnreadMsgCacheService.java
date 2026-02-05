package com.yqrb.service.cache;

import com.yqrb.common.constant.RedisChatKeyConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 未读消息缓存服务（Redis）
 */
@Service
public class UnreadMsgCacheService {

    // 注入Spring封装的Redis操作模板（字符串类型，适合存储未读数量）
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 缓存过期时间：7天（避免无效缓存长期占用Redis内存，可根据业务调整）
    private static final long CACHE_EXPIRE_DAYS = 7;

    /**
     * 未读消息数 +1
     * @param receiverId 接收者ID
     */
    public void incrUnreadMsgCount(String receiverId) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            return;
        }
        String redisKey = RedisChatKeyConstants.buildChatUnreadCountKey(receiverId);
        // 1. Redis INCR 命令：原子性递增（高并发下安全，不会出现计数错乱）
        stringRedisTemplate.opsForValue().increment(redisKey, 1);
        // 2. 设置过期时间（避免Key永久存在），若已存在过期时间则不会覆盖
        stringRedisTemplate.expire(redisKey, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
    }

    /**
     * 未读消息数 -N（原子性递减，避免负数）
     * @param receiverId 接收者ID
     * @param reduceNum 要减少的数量（N>0）
     */
    public void decrUnreadMsgCount(String receiverId, int reduceNum) {
        if (receiverId == null || receiverId.trim().isEmpty() || reduceNum <= 0) {
            return;
        }
        String redisKey = RedisChatKeyConstants.buildChatUnreadCountKey(receiverId);
        // 1. 先查询当前缓存中的数量（避免递减后出现负数）
        String currentCount = stringRedisTemplate.opsForValue().get(redisKey);
        if (currentCount == null) {
            return; // 缓存中无数据，无需处理（后续查询会从DB同步）
        }
        long currentNum = Long.parseLong(currentCount);
        if (currentNum <= reduceNum) {
            // 2. 若当前数量 <= 要减少的数量，直接删除Key（等价于清零）
            stringRedisTemplate.delete(redisKey);
        } else {
            // 3. 原子性递减指定数量
            stringRedisTemplate.opsForValue().increment(redisKey, -reduceNum);
        }
    }

    /**
     * 直接清零接收者的未读消息数（会话维度批量已读时可使用，更高效）
     * @param receiverId 接收者ID
     */
    public void clearUnreadMsgCount(String receiverId) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            return;
        }
        String redisKey = RedisChatKeyConstants.buildChatUnreadCountKey(receiverId);
        stringRedisTemplate.delete(redisKey);
    }

    /**
     * 查询未读消息数（先查Redis，再查DB，最后更新缓存）
     * @param receiverId 接收者ID
     * @param dbQueryFunc 数据库查询未读消息数的函数（回调，解耦DB查询逻辑）
     * @return 未读消息总数
     */
    public long getUnreadMsgCount(String receiverId, java.util.function.Supplier<Long> dbQueryFunc) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            return 0L;
        }
        String redisKey = RedisChatKeyConstants.buildChatUnreadCountKey(receiverId);

        // 1. 先查Redis缓存，有数据直接返回（高性能）
        String cacheCount = stringRedisTemplate.opsForValue().get(redisKey);
        if (cacheCount != null) {
            return Long.parseLong(cacheCount);
        }

        // 2. Redis无数据，调用回调函数查询数据库
        long dbCount = dbQueryFunc == null ? 0L : dbQueryFunc.get();

        // 3. 更新Redis缓存（方便下次查询直接命中）
        if (dbCount > 0) {
            stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(dbCount), CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
        }

        // 4. 返回数据库查询结果
        return dbCount;
    }
}