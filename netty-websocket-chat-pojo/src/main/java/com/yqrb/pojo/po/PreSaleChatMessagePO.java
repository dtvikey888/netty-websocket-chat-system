package com.yqrb.pojo.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 售前咨询聊天记录PO（对应数据库表 pre_sale_chat_message）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreSaleChatMessagePO {
    private Long id; // 主键ID
    private String msgId; // 售前消息唯一标识
    private String senderId; // 发送者ID（用户ID/客服ID）
    private String senderType; // 发送者类型：USER-用户，CS-客服，SYSTEM-系统
    private String receiverId; // 接收者ID
    private String content; // 消息内容
    private String msgType; // 消息类型：TEXT-文本，SYSTEM_TIP-系统提示
    private String attachmentPath; // 附件路径（多张用逗号分隔）
    private String preSaleSessionId; // 售前专属会话ID
    private Date sendTime; // 消息发送时间
    private Integer isRead; // 是否已读：0-未读，1-已读
    private Date createTime; // 记录创建时间

    // 常量定义（与现有风格保持一致，避免硬编码）
    public static final String SENDER_TYPE_USER = "USER";
    public static final String SENDER_TYPE_CS = "CS";
    public static final String SENDER_TYPE_SYSTEM = "SYSTEM";

    public static final String MSG_TYPE_TEXT = "TEXT";
    public static final String MSG_TYPE_SYSTEM_TIP = "SYSTEM_TIP";

    public static final Integer IS_READ_NO = 0;
    public static final Integer IS_READ_YES = 1;
}