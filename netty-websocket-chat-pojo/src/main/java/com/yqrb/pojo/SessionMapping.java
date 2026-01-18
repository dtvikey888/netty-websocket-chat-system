package com.yqrb.pojo;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Table(name = "session_mapping")
public class SessionMapping {
    /**
     * 主键ID
     */
    @Id
    private Long id;

    /**
     * 会话唯一标识（关联app_id）
     */
    @Column(name = "session_id")
    private String sessionId;

    /**
     * 关联申请ID
     */
    @Column(name = "app_id")
    private String appId;

    /**
     * 用户ID
     */
    @Column(name = "user_id")
    private String userId;

    /**
     * 客服ID
     */
    @Column(name = "service_staff_id")
    private String serviceStaffId;

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
     * 获取会话唯一标识（关联app_id）
     *
     * @return session_id - 会话唯一标识（关联app_id）
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话唯一标识（关联app_id）
     *
     * @param sessionId 会话唯一标识（关联app_id）
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 获取关联申请ID
     *
     * @return app_id - 关联申请ID
     */
    public String getAppId() {
        return appId;
    }

    /**
     * 设置关联申请ID
     *
     * @param appId 关联申请ID
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * 获取用户ID
     *
     * @return user_id - 用户ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 设置用户ID
     *
     * @param userId 用户ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * 获取客服ID
     *
     * @return service_staff_id - 客服ID
     */
    public String getServiceStaffId() {
        return serviceStaffId;
    }

    /**
     * 设置客服ID
     *
     * @param serviceStaffId 客服ID
     */
    public void setServiceStaffId(String serviceStaffId) {
        this.serviceStaffId = serviceStaffId;
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