package com.yqrb.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
//聊天消息表实体
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageVO {
    private Long id; // 主键ID
    private String msgId; // 消息唯一标识
    private String senderId; // 发送者ID
    private String senderType; // 发送者类型 USER-用户，CS-客服，SYSTEM-系统
    private String receiverId; // 接收者ID
    private String attachmentPath; // 附件路径（多张用逗号分隔，存储对象存储地址）
    private String content; // 消息内容
    private String msgType; // 消息类型 TEXT-文本，PAY_REMIND-付款提醒，SYSTEM_TIP-系统提示
    private String sessionId; // 会话ID
    private Date sendTime; // 发送时间
    private Integer isRead; // 是否已读 0-未读 1-已读
    private Date createTime; // 创建时间

    // 枚举常量
    public static final String SENDER_TYPE_USER = "USER"; // 普通用户
    public static final String SENDER_TYPE_CS = "CS"; // 客服
    public static final String SENDER_TYPE_SYSTEM = "SYSTEM"; // 系统

    public static final String MSG_TYPE_TEXT = "TEXT"; // 文本消息
    public static final String MSG_TYPE_PAY_REMIND = "PAY_REMIND"; // 付款提醒
    public static final String MSG_TYPE_SYSTEM_TIP = "SYSTEM_TIP"; // 系统提示

    public static final Integer IS_READ_NO = 0; // 未读
    public static final Integer IS_READ_YES = 1; // 已读

}