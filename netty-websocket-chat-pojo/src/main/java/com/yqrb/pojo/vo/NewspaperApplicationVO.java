package com.yqrb.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
//登报申请表实体
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewspaperApplicationVO {
    private Long id; // 主键ID
    private String appId; // 申请唯一标识
    private String userId; // 用户ID
    private String serviceStaffId; // 客服ID
    private String userName; // 申请人姓名
    private String userPhone; // 申请人电话
    private String certType; // 登报类型
    private String sealRe; // 是否重新刻章（yes/no）
    private BigDecimal payAmount; // 应付金额
    private String orderNo; // 订单编号
    private String status; // 申请状态
    private String auditRemark; // 审核备注/驳回原因
    private Date submitTime; // 提交时间
    private Date auditTime; // 审核时间
    private Date payTime; // 支付时间
    private Date createTime; // 创建时间
    private Date updateTime; // 更新时间

    // 申请状态枚举（简化业务判断）
    public static final String STATUS_PENDING = "PENDING"; // 待审核
    public static final String STATUS_AUDITED = "AUDITED"; // 已通过
    public static final String STATUS_PAID = "PAID"; // 已支付
    public static final String STATUS_REJECTED = "REJECTED"; // 已驳回
}