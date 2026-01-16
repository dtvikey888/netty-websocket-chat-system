package com.yqrb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import tk.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
//扫描mybatis通用mapper所在的包
@MapperScan(basePackages = "com.yqrb.mapper")
//扫描所有包以及相关组件包
//@EnableTransactionManagement
//开启定时执行功能
@EnableScheduling
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        log.info("SpringBoot + Netty WebSocket 后端启动成功（纯内存版，无数据库）");
        log.info("HTTP 接口端口：8080，WebSocket 端口：8081");
    }
}
