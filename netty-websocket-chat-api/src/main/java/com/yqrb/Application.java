package com.yqrb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration; // 导入数据源自动配置类

/**
 * Spring Boot 启动类
 * 核心修改：exclude = DataSourceAutoConfiguration.class，禁用数据源自动配置
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}) // 排除数据源自动配置，跳过数据库初始化
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}