package com.yqrb.controller;

import com.yqrb.pojo.OfflineMsg;
import com.yqrb.pojo.query.OfflineMsgQueryParam;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.OfflineMsgService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 离线消息Controller（提供前端拉取离线消息接口）
 */
@Slf4j
@RestController
@RequestMapping("/api/websocket/offline")
public class OfflineMsgController {

    @Autowired
    private OfflineMsgService offlineMsgService;

    /**
     * 拉取指定客服的所有未推送离线消息
     * @param serviceStaffId 客服ID
     * @return 未推送离线消息列表
     */
    @GetMapping("/msgs/{serviceStaffId}")
    public Result<List<OfflineMsg>> getOfflineMsgs(@PathVariable String serviceStaffId) {
        try {
            // 构建查询参数：查询该客服的未推送消息（isPushed=0）
            OfflineMsgQueryParam queryParam = new OfflineMsgQueryParam();
            queryParam.setServiceStaffId(serviceStaffId);
            queryParam.setIsPushed(0);

            // 查询离线消息
            List<OfflineMsg> offlineMsgList = offlineMsgService.getOfflineMsgList(queryParam);

            // 标记消息为已推送（避免重复拉取）
            offlineMsgService.markOfflineMsgAsPushed(serviceStaffId);

            // 返回结果
            return Result.success(offlineMsgList);
        } catch (Exception e) {
            log.error("【拉取离线消息异常】客服ID：{}，异常信息：{}", serviceStaffId, e.getMessage(), e);
            return Result.error("拉取离线消息失败，请稍后重试");
        }
    }
}