package com.yqrb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration; // 导入数据源自动配置类
import org.springframework.context.annotation.ComponentScan;
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
//@ComponentScan(basePackages = {"com.imooc","com.yueqing","com.leqing","org.n3r.idworker"})
//@EnableTransactionManagement
//开启定时执行功能
@EnableScheduling
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}