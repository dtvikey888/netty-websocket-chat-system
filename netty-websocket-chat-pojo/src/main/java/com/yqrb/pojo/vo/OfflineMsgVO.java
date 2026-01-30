package com.yqrb.pojo.vo;

import lombok.Data;
import java.util.Date;
/**
 * 客服离线消息POJO（对应offline_msg表）
 */
@Data
public class OfflineMsgVO {
    /**
     * 主键自增ID
     */
    private Long id;

    /**
     * 客服ID（关联customer_service表的service_staff_id）
     */
    private String serviceStaffId;

    /**
     * 消息类型（如：SYSTEM_NEW_APPLICATION 新登报申请）
     */
    private String msgType;

    /**
     * 登报申请ID（关联newspaper_application表的app_id）
     */
    private String appId;

    /**
     * 是否已推送补偿（0=未推送，1=已推送）
     */
    private Integer isPushed;

    /**
     * 消息创建时间
     */
    private Date createTime;

    /**
     * 消息更新时间
     */
    private Date updateTime;

    /**
     * 消息内容（新申请提醒文本，与WebSocket推送内容一致）
     */
    private String msgContent;
}
