package com.yqrb.pojo;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;

@Table(name = "newspaper_application")
public class NewspaperApplication {
    /**
     * 主键ID
     */
    @Id
    private Long id;

    /**
     * 申请唯一标识
     */
    @Column(name = "app_id")
    private String appId;

    /**
     * 用户ID（对应senderId/receiverId）
     */
    @Column(name = "user_id")
    private String userId;

    /**
     * 客服ID（对应senderId/receiverId）
     */
    @Column(name = "service_staff_id")
    private String serviceStaffId;

    /**
     * 申请人姓名
     */
    @Column(name = "user_name")
    private String userName;

    /**
     * 申请人电话
     */
    @Column(name = "user_phone")
    private String userPhone;

    /**
     * 登报类型
     */
    @Column(name = "cert_type")
    private String certType;

    /**
     * 是否重新刻章（yes/no）
     */
    @Column(name = "seal_re")
    private String sealRe;

    /**
     * 应付金额
     */
    @Column(name = "pay_amount")
    private BigDecimal payAmount;

    /**
     * 订单编号
     */
    @Column(name = "order_no")
    private String orderNo;

    /**
     * 状态：PENDING-待审核，AUDITED-已通过，PAID-已支付，REJECTED-已驳回
     * REFUND_APPLIED 用户已发起退款申请，待客服审核（核心状态）
     * REFUNDED 客服审核通过，退款操作已完成（核心状态，最终态）
     * REFUND_REJECTED	客服审核驳回用户的退款申请（可选状态，优化体验）
     */
    private String status;

    /**
     * 审核备注/驳回原因
     */
    @Column(name = "audit_remark")
    private String auditRemark;

    /**
     * 提交时间
     */
    @Column(name = "submit_time")
    private Date submitTime;

    /**
     * 审核时间
     */
    @Column(name = "audit_time")
    private Date auditTime;

    /**
     * 支付时间
     */
    @Column(name = "pay_time")
    private Date payTime;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private Date updateTime;

    /**
     * 获取主键ID
     *
     * @return id - 主键ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置主键ID
     *
     * @param id 主键ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取申请唯一标识
     *
     * @return app_id - 申请唯一标识
     */
    public String getAppId() {
        return appId;
    }

    /**
     * 设置申请唯一标识
     *
     * @param appId 申请唯一标识
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * 获取用户ID（对应senderId/receiverId）
     *
     * @return user_id - 用户ID（对应senderId/receiverId）
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 设置用户ID（对应senderId/receiverId）
     *
     * @param userId 用户ID（对应senderId/receiverId）
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * 获取客服ID（对应senderId/receiverId）
     *
     * @return service_staff_id - 客服ID（对应senderId/receiverId）
     */
    public String getServiceStaffId() {
        return serviceStaffId;
    }

    /**
     * 设置客服ID（对应senderId/receiverId）
     *
     * @param serviceStaffId 客服ID（对应senderId/receiverId）
     */
    public void setServiceStaffId(String serviceStaffId) {
        this.serviceStaffId = serviceStaffId;
    }

    /**
     * 获取申请人姓名
     *
     * @return user_name - 申请人姓名
     */
    public String getUserName() {
        return userName;
    }

    /**
     * 设置申请人姓名
     *
     * @param userName 申请人姓名
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * 获取申请人电话
     *
     * @return user_phone - 申请人电话
     */
    public String getUserPhone() {
        return userPhone;
    }

    /**
     * 设置申请人电话
     *
     * @param userPhone 申请人电话
     */
    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    /**
     * 获取登报类型
     *
     * @return cert_type - 登报类型
     */
    public String getCertType() {
        return certType;
    }

    /**
     * 设置登报类型
     *
     * @param certType 登报类型
     */
    public void setCertType(String certType) {
        this.certType = certType;
    }

    /**
     * 获取是否重新刻章（yes/no）
     *
     * @return seal_re - 是否重新刻章（yes/no）
     */
    public String getSealRe() {
        return sealRe;
    }

    /**
     * 设置是否重新刻章（yes/no）
     *
     * @param sealRe 是否重新刻章（yes/no）
     */
    public void setSealRe(String sealRe) {
        this.sealRe = sealRe;
    }

    /**
     * 获取应付金额
     *
     * @return pay_amount - 应付金额
     */
    public BigDecimal getPayAmount() {
        return payAmount;
    }

    /**
     * 设置应付金额
     *
     * @param payAmount 应付金额
     */
    public void setPayAmount(BigDecimal payAmount) {
        this.payAmount = payAmount;
    }

    /**
     * 获取订单编号
     *
     * @return order_no - 订单编号
     */
    public String getOrderNo() {
        return orderNo;
    }

    /**
     * 设置订单编号
     *
     * @param orderNo 订单编号
     */
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    /**
     * 获取状态：PENDING-待审核，AUDITED-已通过，PAID-已支付，REJECTED-已驳回
     *
     * @return status - 状态：PENDING-待审核，AUDITED-已通过，PAID-已支付，REJECTED-已驳回
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置状态：PENDING-待审核，AUDITED-已通过，PAID-已支付，REJECTED-已驳回
     *
     * @param status 状态：PENDING-待审核，AUDITED-已通过，PAID-已支付，REJECTED-已驳回
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取审核备注/驳回原因
     *
     * @return audit_remark - 审核备注/驳回原因
     */
    public String getAuditRemark() {
        return auditRemark;
    }

    /**
     * 设置审核备注/驳回原因
     *
     * @param auditRemark 审核备注/驳回原因
     */
    public void setAuditRemark(String auditRemark) {
        this.auditRemark = auditRemark;
    }

    /**
     * 获取提交时间
     *
     * @return submit_time - 提交时间
     */
    public Date getSubmitTime() {
        return submitTime;
    }

    /**
     * 设置提交时间
     *
     * @param submitTime 提交时间
     */
    public void setSubmitTime(Date submitTime) {
        this.submitTime = submitTime;
    }

    /**
     * 获取审核时间
     *
     * @return audit_time - 审核时间
     */
    public Date getAuditTime() {
        return auditTime;
    }

    /**
     * 设置审核时间
     *
     * @param auditTime 审核时间
     */
    public void setAuditTime(Date auditTime) {
        this.auditTime = auditTime;
    }

    /**
     * 获取支付时间
     *
     * @return pay_time - 支付时间
     */
    public Date getPayTime() {
        return payTime;
    }

    /**
     * 设置支付时间
     *
     * @param payTime 支付时间
     */
    public void setPayTime(Date payTime) {
        this.payTime = payTime;
    }

    /**
     * 获取创建时间
     *
     * @return create_time - 创建时间
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * 设置创建时间
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取更新时间
     *
     * @return update_time - 更新时间
     */
    public Date getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置更新时间
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}