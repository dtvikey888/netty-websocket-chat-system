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
        // 修复核心：替换通配符*为具体前端域名，支持多个域名（按需添加）
        // 1. 你的Canvas前端预览地址（必须添加，解决当前跨域问题）
        corsConfiguration.addAllowedOrigin("http://localhost:5500");
        // 2. 你的业务域名（保留你原有项目的合法域名，按需启用/添加）
        corsConfiguration.addAllowedOrigin("http://localhost:8088");
        corsConfiguration.addAllowedOrigin("http://s.txy.yqrb.com.cn");
        corsConfiguration.addAllowedOrigin("http://admin.txy.yqrb.com.cn");
        corsConfiguration.addAllowedOrigin("http://www.yqrb.com.cn");
        corsConfiguration.addAllowedOrigin("http://admin.yqrb.com.cn");
        corsConfiguration.addAllowedOrigin("http://s.yqrb.com.cn");
        corsConfiguration.addAllowedOrigin("http://img.yqrb.com.cn");

        // 允许所有请求头（包含你的自定义头ReceiverId、Request-Id）
        corsConfiguration.addAllowedHeader("*");
        // 允许所有请求方法（GET、POST、PUT、DELETE等，适配你的提交接口POST请求）
        corsConfiguration.addAllowedMethod("*");
        // 允许携带Cookie（对应前端fetch的credentials: "include"，必须启用）
        corsConfiguration.setAllowCredentials(true);
        // 预检请求缓存时间（3600秒），减少重复预检请求，提升性能
        corsConfiguration.setMaxAge(3600L);
        return corsConfiguration;
    }

    @Bean
    public CorsFilter corsFilter(){
        // 为URL添加映射路径，注册跨域配置
        UrlBasedCorsConfigurationSource corsSource = new UrlBasedCorsConfigurationSource();
        // 对所有接口路径（/**）应用跨域配置规则
        corsSource.registerCorsConfiguration("/**",buildConfig());
        // 返回跨域过滤器，交由Spring容器管理
        return new CorsFilter(corsSource);
    }
}