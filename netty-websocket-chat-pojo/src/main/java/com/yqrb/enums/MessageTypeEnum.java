package com.yqrb.enums;

/**
 * WebSocket 消息类型枚举
 */
public enum MessageTypeEnum {
    /** 文本消息 */
    TEXT,
    /** 连接成功通知 */
    CONNECT_SUCCESS,
    /** 用户下线通知 */
    USER_OFFLINE,
    /** 系统提示 */
    SYSTEM_TIP,
    /** 付款提醒 */
    PAY_REMIND,
    /** 驳回提醒 */
    REJECT_REMIND
}
