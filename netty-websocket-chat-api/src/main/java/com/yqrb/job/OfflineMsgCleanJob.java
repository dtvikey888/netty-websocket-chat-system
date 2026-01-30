package com.yqrb.job;

import com.yqrb.mapper.OfflineMsgMapper;
import com.yqrb.mapper.OfflineMsgMapperCustom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 离线消息清理定时任务
 */
@Slf4j
@Component
public class OfflineMsgCleanJob {

    @Autowired
    private OfflineMsgMapperCustom offlineMsgMapperCustom;

    /**
     * 每天凌晨2点清理：已推送（is_pushed=1）且超过7天的离线消息
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanOfflineMsg() {
        try {
            // 执行清理SQL（需在OfflineMsgMapper中添加对应方法）
            int deleteCount = offlineMsgMapperCustom.cleanExpiredOfflineMsg(7); // 7天
            log.info("【离线消息清理完成】共清理 {} 条过期已推送消息", deleteCount);
        } catch (Exception e) {
            log.error("【离线消息清理异常】异常信息：{}", e.getMessage(), e);
        }
    }
}