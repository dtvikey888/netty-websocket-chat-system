package com.yqrb.pojo;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Table(name = "offline_msg")
public class OfflineMsg {
    /**
     * 主键自增ID
     */
    @Id
    private Long id;

    /**
     * 客服ID（关联customer_service表的service_staff_id）
     */
    @Column(name = "service_staff_id")
    private String serviceStaffId;

    /**
     * 消息类型（如：SYSTEM_NEW_APPLICATION 新登报申请）
     */
    @Column(name = "msg_type")
    private String msgType;

    /**
     * 登报申请ID（关联newspaper_application表的app_id）
     */
    @Column(name = "app_id")
    private String appId;

    /**
     * 是否已推送补偿（0=未推送，1=已推送）
     */
    @Column(name = "is_pushed")
    private Boolean isPushed;

    /**
     * 消息创建时间
     */
    @Column(name = "create_time")
    private Date createTime;

    /**
     * 消息更新时间
     */
    @Column(name = "update_time")
    private Date updateTime;

    /**
     * 消息内容（新申请提醒文本，与WebSocket推送内容一致）
     */
    @Column(name = "msg_content")
    private String msgContent;

    /**
     * 获取主键自增ID
     *
     * @return id - 主键自增ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置主键自增ID
     *
     * @param id 主键自增ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取客服ID（关联customer_service表的service_staff_id）
     *
     * @return service_staff_id - 客服ID（关联customer_service表的service_staff_id）
     */
    public String getServiceStaffId() {
        return serviceStaffId;
    }

    /**
     * 设置客服ID（关联customer_service表的service_staff_id）
     *
     * @param serviceStaffId 客服ID（关联customer_service表的service_staff_id）
     */
    public void setServiceStaffId(String serviceStaffId) {
        this.serviceStaffId = serviceStaffId;
    }

    /**
     * 获取消息类型（如：SYSTEM_NEW_APPLICATION 新登报申请）
     *
     * @return msg_type - 消息类型（如：SYSTEM_NEW_APPLICATION 新登报申请）
     */
    public String getMsgType() {
        return msgType;
    }

    /**
     * 设置消息类型（如：SYSTEM_NEW_APPLICATION 新登报申请）
     *
     * @param msgType 消息类型（如：SYSTEM_NEW_APPLICATION 新登报申请）
     */
    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    /**
     * 获取登报申请ID（关联newspaper_application表的app_id）
     *
     * @return app_id - 登报申请ID（关联newspaper_application表的app_id）
     */
    public String getAppId() {
        return appId;
    }

    /**
     * 设置登报申请ID（关联newspaper_application表的app_id）
     *
     * @param appId 登报申请ID（关联newspaper_application表的app_id）
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * 获取是否已推送补偿（0=未推送，1=已推送）
     *
     * @return is_pushed - 是否已推送补偿（0=未推送，1=已推送）
     */
    public Boolean getIsPushed() {
        return isPushed;
    }

    /**
     * 设置是否已推送补偿（0=未推送，1=已推送）
     *
     * @param isPushed 是否已推送补偿（0=未推送，1=已推送）
     */
    public void setIsPushed(Boolean isPushed) {
        this.isPushed = isPushed;
    }

    /**
     * 获取消息创建时间
     *
     * @return create_time - 消息创建时间
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * 设置消息创建时间
     *
     * @param createTime 消息创建时间
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取消息更新时间
     *
     * @return update_time - 消息更新时间
     */
    public Date getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置消息更新时间
     *
     * @param updateTime 消息更新时间
     */
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 获取消息内容（新申请提醒文本，与WebSocket推送内容一致）
     *
     * @return msg_content - 消息内容（新申请提醒文本，与WebSocket推送内容一致）
     */
    public String getMsgContent() {
        return msgContent;
    }

    /**
     * 设置消息内容（新申请提醒文本，与WebSocket推送内容一致）
     *
     * @param msgContent 消息内容（新申请提醒文本，与WebSocket推送内容一致）
     */
    public void setMsgContent(String msgContent) {
        this.msgContent = msgContent;
    }
}