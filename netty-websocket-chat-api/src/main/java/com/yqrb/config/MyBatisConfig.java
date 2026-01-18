package com.yqrb.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis全局配置类
 */
@Configuration
@MapperScan(basePackages = "com.yqrb.mapper")
public class MyBatisConfig {
}