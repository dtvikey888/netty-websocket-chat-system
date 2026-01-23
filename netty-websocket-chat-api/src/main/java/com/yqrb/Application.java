package com.yqrb;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot 启动类
 * 核心修改：exclude = DataSourceAutoConfiguration.class，禁用数据源自动配置
 */
/**
 * @ClassName $ {NAME}
 * @Description TODO
 * @Author fjw
 * @Date 2020/2/16 9:16 PM
 * @Version 1.0
 **/
@SpringBootApplication
//扫描mybatis通用mapper所在的包
@MapperScan(basePackages = "com.yqrb.mapper")
//扫描所有包以及相关组件包
//@ComponentScan(basePackages = {"com.yqrb","com.yueqing","com.leqing","org.n3r.idworker"})
//@EnableTransactionManagement
//开启定时执行功能
@EnableScheduling
@Slf4j // 引入lombok日志，替换System.out，更规范
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        System.out.println("线上登报办理系统（完整版）启动成功：");
        System.out.println("  - HTTP端口：8088");
        System.out.println("  - WebSocket端口：8081");
        System.out.println("  - 核心功能：仅保留压缩图片，自动删除原始图片");
    }
}