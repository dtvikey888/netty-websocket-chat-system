package com.yqrb.controller;


import com.yqrb.dto.AuditRequestDTO;
import com.yqrb.dto.ResultDTO;
import com.yqrb.utils.WebSocketMemoryManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 登报审核 HTTP 接口（纯内存版，无数据库操作）
 */
@Slf4j
@RestController
@RequestMapping("/api/audit")
public class AuditController {
    @Autowired
    private WebSocketMemoryManager webSocketMemoryManager;

    /**
     * 获取待审核申请列表（纯内存查询）
     */
    @GetMapping("/list")
    public ResultDTO<List<WebSocketMemoryManager.ApplicationInfo>> getPendingApplications() {
        try {
            List<WebSocketMemoryManager.ApplicationInfo> pendingList = webSocketMemoryManager.getPendingApplications();
            return ResultDTO.success(pendingList, "查询待审核申请成功（纯内存）");
        } catch (Exception e) {
            log.error("查询待审核申请失败", e);
            return ResultDTO.fail("查询待审核申请失败");
        }
    }

    /**
     * 审核通过（设置付款金额，推送付款提醒，纯内存操作）
     */
    @PostMapping("/pass")
    public ResultDTO<Map<String, String>> passAudit(@Validated @RequestBody AuditRequestDTO auditRequest) {
        try {
            // 1. 校验付款金额
            String payAmountStr = auditRequest.getPayAmount();
            if (payAmountStr == null || payAmountStr.trim().isEmpty()) {
                return ResultDTO.fail("付款金额不能为空");
            }

            BigDecimal payAmount;
            try {
                payAmount = new BigDecimal(payAmountStr);
            } catch (Exception e) {
                return ResultDTO.fail("付款金额格式错误");
            }

            if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResultDTO.fail("付款金额必须大于0");
            }

            // 2. 纯内存处理审核通过（更新状态 + 推送提醒）
            String appId = auditRequest.getAppId();
            String userId = auditRequest.getUserId();
            log.info("处理申请[{}]审核通过（纯内存），用户[{}]，金额[¥{}]", appId, userId, payAmount);

            String orderNo = webSocketMemoryManager.handleAuditPass(appId, userId, payAmount);

            // 3. 构造返回数据
            Map<String, String> data = new HashMap<>();
            data.put("orderNo", orderNo);
            data.put("appId", appId);
            data.put("payAmount", payAmountStr);

            return ResultDTO.success(data, "审核通过，已推送付款提醒给用户（纯内存）");
        } catch (Exception e) {
            log.error("审核通过处理失败", e);
            return ResultDTO.fail("审核通过失败：" + e.getMessage());
        }
    }

    /**
     * 审核驳回（推送驳回提醒，纯内存操作）
     */
    @PostMapping("/reject")
    public ResultDTO<Void> rejectAudit(@Validated @RequestBody AuditRequestDTO auditRequest) {
        try {
            // 1. 校验驳回原因
            String rejectReason = auditRequest.getRejectReason();
            if (rejectReason == null || rejectReason.trim().isEmpty()) {
                return ResultDTO.fail("驳回原因不能为空");
            }

            // 2. 纯内存处理审核驳回（更新状态 + 推送提醒）
            String appId = auditRequest.getAppId();
            String userId = auditRequest.getUserId();
            log.info("处理申请[{}]审核驳回（纯内存），用户[{}]，原因[{}]", appId, userId, rejectReason);

            webSocketMemoryManager.handleAuditReject(appId, userId, rejectReason);

            return ResultDTO.success("驳回成功，已推送提醒给用户（纯内存）");
        } catch (Exception e) {
            log.error("审核驳回处理失败", e);
            return ResultDTO.fail("审核驳回失败：" + e.getMessage());
        }
    }
}