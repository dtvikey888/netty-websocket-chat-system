package com.yqrb.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring上下文工具类：用于在非Spring容器管理的类中（如Netty Handler）获取Bean
 */
@Component
public class SpringContextUtil implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        SpringContextUtil.applicationContext = context;
    }

    /**
     * 获取Spring容器中的Bean
     * @param clazz Bean的类类型
     * @param <T> 泛型
     * @return Bean实例
     */
    public static <T> T getBean(Class<T> clazz) {
        if (applicationContext == null) {
            throw new RuntimeException("Spring上下文未初始化，无法获取Bean");
        }
        return applicationContext.getBean(clazz);
    }
}