package com.yqrb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
//扫描mybatis通用mapper所在的包
@MapperScan(basePackages = "com.yqrb.mapper")
//扫描所有包以及相关组件包
//@EnableTransactionManagement
//开启定时执行功能
@EnableScheduling
public class Application {


    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }


}
