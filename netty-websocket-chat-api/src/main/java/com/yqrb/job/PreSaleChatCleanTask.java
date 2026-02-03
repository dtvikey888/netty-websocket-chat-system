package com.yqrb.job;

import com.yqrb.service.PreSaleChatMessageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 售前消息定时清理任务
 */
@Component
// 开启定时任务（若项目未开启，需在启动类上添加 @EnableScheduling 注解）
public class PreSaleChatCleanTask {

    @Resource
    private PreSaleChatMessageService preSaleChatMessageService;

    /**
     * 每天凌晨2点执行，清理7天前的售前消息
     * cron表达式：0 0 2 * * ? （秒 分 时 日 月 周）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredPreSaleChatMessage() {
        preSaleChatMessageService.cleanExpiredPreSaleChatMessage();
    }
}