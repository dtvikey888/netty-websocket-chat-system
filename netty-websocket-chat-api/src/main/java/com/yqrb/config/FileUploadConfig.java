package com.yqrb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * 文件上传全局配置类（映射静态资源，支持访问压缩图片）
 */
@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Value("${custom.file.upload.base-path}")
    private String baseUploadPath;

    /**
     * 配置静态资源映射，允许通过HTTP访问压缩图片
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射URL：/images/** -> 实际存储目录
        String resourcePath = "file:" + new File(baseUploadPath).getAbsolutePath() + File.separator;
        registry.addResourceHandler("/images/**")
                .addResourceLocations(resourcePath);
    }
}