package com.yqrb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * @ClassName CorsConfig
 * @Description 跨域配置类：解决前后端分离场景下的跨域请求问题
 * @Author fjw
 * @Date 2020/2/5 3:11 PM
 * @Version 1.0
 **/
@Configuration
public class CorsConfig {

    private CorsConfiguration buildConfig() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // 修正：添加前端实际运行端口（localhost:8080），覆盖你的referer地址
        corsConfiguration.addAllowedOrigin("http://localhost:8080");
        // 保留之前的其他域名，按需启用
        corsConfiguration.addAllowedOrigin("http://localhost:5500");
        corsConfiguration.addAllowedOrigin("http://s.txy.yqrb.com.cn");
        corsConfiguration.addAllowedOrigin("http://admin.txy.yqrb.com.cn");
        corsConfiguration.addAllowedOrigin("http://www.yqrb.com.cn");

        // 允许所有请求头（包含自定义头 receiverid、request-id）
        corsConfiguration.addAllowedHeader("*");
        // 允许所有请求方法（适配 POST 提交接口）
        corsConfiguration.addAllowedMethod("*");
        // 允许携带 Cookie（对应前端 fetch 的 credentials: "include"）
        corsConfiguration.setAllowCredentials(true);
        // 预检请求缓存时间 3600 秒，提升性能
        corsConfiguration.setMaxAge(3600L);
        return corsConfiguration;
    }

    @Bean
    public CorsFilter corsFilter(){
        UrlBasedCorsConfigurationSource corsSource = new UrlBasedCorsConfigurationSource();
        corsSource.registerCorsConfiguration("/**",buildConfig());
        return new CorsFilter(corsSource);
    }
}