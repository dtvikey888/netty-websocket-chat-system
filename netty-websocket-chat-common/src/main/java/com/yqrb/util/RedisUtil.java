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
}