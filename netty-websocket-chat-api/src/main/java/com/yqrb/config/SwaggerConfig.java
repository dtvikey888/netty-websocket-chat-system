package com.yqrb.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Swagger接口文档配置类（接口测试用）
 */
@Configuration
@EnableSwagger2
@ConditionalOnProperty(name = "swagger.enable", havingValue = "true")
public class SwaggerConfig {
    // http://localhost:8088/swagger-ui.html   原路径
    // http://localhost:8088/doc.html   原路径
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                // 扫描控制层包
                .apis(RequestHandlerSelectors.basePackage("com.yqrb.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * 接口文档基本信息
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("线上登报办理系统API文档")
                .description("完整版·仅保留压缩图片，自动删除原始图片")
                .version("1.0.0")
                .build();
    }
}