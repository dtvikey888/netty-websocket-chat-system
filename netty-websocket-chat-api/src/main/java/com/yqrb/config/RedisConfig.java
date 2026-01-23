package com.yqrb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis全局配置类
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 设置连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 设置Key序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        // 设置Value序列化器
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        // 设置Hash Key序列化器
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        // 设置Hash Value序列化器
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        // 初始化配置
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}