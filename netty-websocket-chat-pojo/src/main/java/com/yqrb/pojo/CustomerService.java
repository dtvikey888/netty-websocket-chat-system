package com.yqrb.pojo;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Table(name = "customer_service")
public class CustomerService {
    /**
     * 主键ID
     */
    @Id
    private Long id;

    /**
     * 客服唯一标识（cs_xxx，对应receiverId）
     */
    @Column(name = "service_staff_id")
    private String serviceStaffId;

    /**
     * 客服姓名
     */
    @Column(name = "service_name")
    private String serviceName;

    /**
     * 客服电话
     */
    @Column(name = "service_phone")
    private String servicePhone;

    /**
     * 状态：ONLINE-在线，OFFLINE-离线
     */
    private String status;

    /**
     * 最后登录时间
     */
    @Column(name = "login_time")
    private Date loginTime;

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
     * 获取客服唯一标识（cs_xxx，对应receiverId）
     *
     * @return service_staff_id - 客服唯一标识（cs_xxx，对应receiverId）
     */
    public String getServiceStaffId() {
        return serviceStaffId;
    }

    /**
     * 设置客服唯一标识（cs_xxx，对应receiverId）
     *
     * @param serviceStaffId 客服唯一标识（cs_xxx，对应receiverId）
     */
    public void setServiceStaffId(String serviceStaffId) {
        this.serviceStaffId = serviceStaffId;
    }

    /**
     * 获取客服姓名
     *
     * @return service_name - 客服姓名
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * 设置客服姓名
     *
     * @param serviceName 客服姓名
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * 获取客服电话
     *
     * @return service_phone - 客服电话
     */
    public String getServicePhone() {
        return servicePhone;
    }

    /**
     * 设置客服电话
     *
     * @param servicePhone 客服电话
     */
    public void setServicePhone(String servicePhone) {
        this.servicePhone = servicePhone;
    }

    /**
     * 获取状态：ONLINE-在线，OFFLINE-离线
     *
     * @return status - 状态：ONLINE-在线，OFFLINE-离线
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置状态：ONLINE-在线，OFFLINE-离线
     *
     * @param status 状态：ONLINE-在线，OFFLINE-离线
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取最后登录时间
     *
     * @return login_time - 最后登录时间
     */
    public Date getLoginTime() {
        return loginTime;
    }

    /**
     * 设置最后登录时间
     *
     * @param loginTime 最后登录时间
     */
    public void setLoginTime(Date loginTime) {
        this.loginTime = loginTime;
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