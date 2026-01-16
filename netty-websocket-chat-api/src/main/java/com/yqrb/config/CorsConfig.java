package com.yqrb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * @ClassName $ {NAME}
 * @Description 跨域配置类：解决前后端分离场景下的跨域请求问题
 * @Author fjw
 * @Date 2020/2/5 3:11 PM
 * @Version 1.0
 **/
@Configuration
public class CorsConfig {



//    public CorsConfig() {
//
//    }

//    private CorsConfiguration buildConfig() {
//        CorsConfiguration config = new CorsConfiguration();
//        config.addAllowedOrigin("http://localhost:8080");
//        config.addAllowedOrigin("http://s.txy.yqrb.com.cn");
//        config.addAllowedOrigin("http://admin.txy.yqrb.com.cn");
//        config.addAllowedOrigin("http://www.yqrb.com.cn:8080");
//        config.addAllowedOrigin("http://admin.yqrb.com.cn:8080");
//        config.addAllowedOrigin("http://s.yqrb.com.cn:8080");
//        config.addAllowedOrigin("http://img.yqrb.com.cn:8080");
//        config.addAllowedOrigin("http://www.yqrb.com.cn");
//        config.addAllowedOrigin("http://admin.yqrb.com.cn");
//        config.addAllowedOrigin("http://s.yqrb.com.cn");
//        config.addAllowedOrigin("http://img.yqrb.com.cn");
//
//        //设置是否发送cookie信息
//        config.setAllowCredentials(true);
//        //设置允许请求的方式
//        config.addAllowedMethod("*");
//        //设置允许的header
//        config.addAllowedHeader("*");
//        return config;
//    }

    private CorsConfiguration buildConfig() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // 允许所有来源跨域（开发环境便捷配置，生产环境可替换为指定域名，如"http://admin.yqrb.com.cn"）
        corsConfiguration.addAllowedOrigin("*");
        // 允许所有请求头
        corsConfiguration.addAllowedHeader("*");
        // 允许所有请求方法（GET、POST、PUT、DELETE等）
        corsConfiguration.addAllowedMethod("*");
        // 允许携带Cookie（跨域请求需同步Cookie时启用）
        corsConfiguration.setAllowCredentials(true);//这两句不加不能跨域上传文件
        // 预检请求缓存时间（3600秒），减少重复预检请求
        corsConfiguration.setMaxAge(3600l);//加上去就可以了
        return corsConfiguration;
    }

    @Bean
    public CorsFilter corsFilter(){
        //1. 添加cros配置信息
//        CorsConfiguration config = new CorsConfiguration();
//        config.addAllowedOrigin("http://localhost:8080");
//        config.addAllowedOrigin("http://s.txy.yqrb.com.cn");
//        config.addAllowedOrigin("http://admin.txy.yqrb.com.cn");
//        config.addAllowedOrigin("http://www.yqrb.com.cn:8080");
//        config.addAllowedOrigin("http://admin.yqrb.com.cn:8080");
//        config.addAllowedOrigin("http://s.yqrb.com.cn:8080");
//        config.addAllowedOrigin("http://img.yqrb.com.cn:8080");
//        config.addAllowedOrigin("http://www.yqrb.com.cn");
//        config.addAllowedOrigin("http://admin.yqrb.com.cn");
//        config.addAllowedOrigin("http://s.yqrb.com.cn");
//        config.addAllowedOrigin("http://img.yqrb.com.cn");
//
//        //设置是否发送cookie信息
//        config.setAllowCredentials(true);
//        //设置允许请求的方式
//        config.addAllowedMethod("*");
//        //设置允许的header
//        config.addAllowedHeader("*");
        //2.为url添加映射路径、为所有URL注册跨域配置
        UrlBasedCorsConfigurationSource corsSource = new UrlBasedCorsConfigurationSource();
        //corsSource.registerCorsConfiguration("/**",config);// 对接口配置跨域设置
        corsSource.registerCorsConfiguration("/**",buildConfig());// 对接口配置跨域设置
        //3.返回重新定义好的corsSource
        return new CorsFilter(corsSource);
    }


}
