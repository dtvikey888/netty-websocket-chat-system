package com.yqrb.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 登报审核请求参数 DTO（纯内存校验，无数据库关联）
 */
@Data
public class AuditRequestDTO {
    /** 申请ID */
    @NotBlank(message = "申请ID不能为空")
    private String appId;

    /** 用户ID */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /** 付款金额（仅审核通过时必填，正数，最多两位小数） */
    @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "付款金额格式错误（正数，最多两位小数）")
    private String payAmount;

    /** 驳回原因（仅审核驳回时必填） */
    private String rejectReason;

    /** 审核备注（可选） */
    private String auditRemark;
}
