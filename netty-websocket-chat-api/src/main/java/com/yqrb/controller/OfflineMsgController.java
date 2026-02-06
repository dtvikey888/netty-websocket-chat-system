package com.yqrb.controller;

import com.yqrb.pojo.query.OfflineMsgQueryParam;
import com.yqrb.pojo.vo.OfflineMsgVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.OfflineMsgService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 离线消息Controller（提供前端拉取离线消息接口）
 * 改造后：按「客服ID+sessionId」精准拉取/标记当前会话的离线消息
 */
@Slf4j
@RestController
@RequestMapping("/api/websocket/offline")
public class OfflineMsgController {

    @Autowired
    private OfflineMsgService offlineMsgService;

    /**
     * 拉取【指定客服+指定会话】的所有未推送离线消息（改造后：精准拉取，避免跨会话）
     * @param serviceStaffId 客服ID
     * @param sessionId 业务会话ID【新增】：必须传入，精准定位当前会话的离线消息
     * @return 当前会话的未推送离线消息列表
     */
    @GetMapping("/msgs/{serviceStaffId}")
    @ApiOperation("拉取【指定客服+指定会话】的所有未推送离线消息（改造后：精准拉取，避免跨会话）")
    public Result<List<OfflineMsgVO>> getOfflineMsgs(
            @PathVariable String serviceStaffId,
            @RequestParam String sessionId // 新增：前端传入当前业务会话ID
    ) {
        try {
            // 1. 基础参数校验：避免空参数导致查询/标记异常
            if (sessionId == null || sessionId.trim().isEmpty()) {
                log.warn("【拉取离线消息】客服ID：{}，会话ID为空，拒绝拉取", serviceStaffId);
                return Result.paramError("会话ID不能为空");
            }

            // 2. 构建查询参数：按「客服ID+sessionId+未推送」精准查询
            OfflineMsgQueryParam queryParam = new OfflineMsgQueryParam();
            queryParam.setServiceStaffId(serviceStaffId);
            queryParam.setSessionId(sessionId); // 核心：仅查当前会话的消息
            queryParam.setIsPushed(0);

            // 3. 精准查询当前会话的未推送离线消息
            List<OfflineMsgVO> offlineMsgList = offlineMsgService.getOfflineMsgList(queryParam);
            log.info("【拉取离线消息】客服ID：{}，会话ID：{}，查询到未推送消息{}条",
                    serviceStaffId, sessionId, offlineMsgList.size());

            // 4. 精准标记：仅将「当前客服+当前会话」的离线消息标记为已推送
            offlineMsgService.markOfflineMsgAsPushed(serviceStaffId, sessionId);

            // 5. 返回当前会话的离线消息列表
            return Result.success(offlineMsgList);
        } catch (Exception e) {
            log.error("【拉取离线消息异常】客服ID：{}，会话ID：{}，异常信息：{}",
                    serviceStaffId, sessionId, e.getMessage(), e);
            return Result.error("拉取离线消息失败，请稍后重试");
        }
    }
}