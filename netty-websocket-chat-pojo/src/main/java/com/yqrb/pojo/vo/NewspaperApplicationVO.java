package com.yqrb.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

//登报申请表实体
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewspaperApplicationVO {
    private Long id; // 主键ID
    private String appId; // 申请唯一标识
    @NotBlank(message = "用户ID不能为空") // 非空且不能是空白字符串
    private String userId; // 用户ID
    private String serviceStaffId; // 客服ID
    @NotBlank(message = "用户名不能为空")
    private String userName; // 申请人姓名
    @NotBlank(message = "用户手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") // 校验11位手机号格式
    private String userPhone; // 申请人电话
    private String certType; // 登报类型
    private String sealRe; // 是否重新刻章（yes/no）
    private BigDecimal payAmount; // 应付金额
    private String orderNo; // 订单编号
    private String status; // 申请状态
    private String auditRemark; // 审核备注/驳回原因

    // 核心修改：添加日期格式化注解，适配yyyy-MM-dd HH:mm:ss格式
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date submitTime; // 提交时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date auditTime; // 审核时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date payTime; // 支付时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime; // 创建时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime; // 更新时间

    // 申请状态枚举（简化业务判断）
    public static final String STATUS_PENDING = "PENDING"; // 待审核
    public static final String STATUS_AUDITED = "AUDITED"; // 已通过
    public static final String STATUS_PAID = "PAID"; // 已支付
    public static final String STATUS_REJECTED = "REJECTED"; // 已驳回

    // 新增：退款相关状态
    public static final String STATUS_REFUND_APPLIED = "REFUND_APPLIED";
    public static final String STATUS_REFUNDED = "REFUNDED";
    public static final String STATUS_REFUND_REJECTED = "REFUND_REJECTED";

    // 补充：新增退款相关字段（需同步在数据库表中添加对应字段）
    private BigDecimal refundAmount; // 退款金额（一般等于支付金额，特殊场景可部分退款）
    private Date refundApplyTime; // 退款申请时间
    private Date refundTime; // 实际退款完成时间
    private String refundRemark; // 退款备注（用户申请理由/客服审核备注）

}