package com.yqrb.util;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 存入Redis并设置过期时间
    public boolean set(String key, Object value, long expireSeconds) {
        if (StrUtil.isBlank(key) || value == null) {
            return false;
        }
        try {
            redisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 从Redis获取数据
    public Object get(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        return redisTemplate.opsForValue().get(key);
    }

    // 删除Redis中的数据
    public boolean delete(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        try {
            return redisTemplate.delete(key);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 刷新过期时间
    public boolean refreshExpire(String key, long expireSeconds) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        try {
            redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 存入Redis（无过期时间）
    public boolean setWithoutExpire(String key, Object value) {
        if (StrUtil.isBlank(key) || value == null) {
            return false;
        }
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ========== 新增：List相关方法（离线消息必备） ==========
    /**
     * 向List尾部添加元素，并设置过期时间
     */
    public boolean lSet(String key, Object value, long expireSeconds) {
        if (StrUtil.isBlank(key) || value == null) {
            return false;
        }
        try {
            redisTemplate.opsForList().rightPush(key, value);
            // 只在第一次添加时设置过期时间
            if (redisTemplate.getExpire(key) < 0) {
                redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取List的长度
     */
    public long lGetListSize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0;
        }
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取List指定索引的元素
     */
    public Object lIndex(String key, long index) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 判断key是否存在
     */
    public boolean hasKey(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除List中指定范围的元素
     */
    public void lTrim(String key, long start, long end) {
        if (StrUtil.isBlank(key)) {
            return;
        }
        try {
            redisTemplate.opsForList().trim(key, start, end);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}