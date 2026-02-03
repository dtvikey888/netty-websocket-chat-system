package com.yqrb.pojo.vo;

import com.yqrb.pojo.po.PreSaleChatMessagePO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 售前咨询聊天记录VO（前后端交互使用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreSaleChatMessageVO {
    private Long id; // 主键ID
    private String msgId; // 售前消息唯一标识
    private String senderId; // 发送者ID
    private String senderType; // 发送者类型
    private String receiverId; // 接收者ID
    private String content; // 消息内容
    private String msgType; // 消息类型
    private String attachmentPath; // 附件路径
    private String preSaleSessionId; // 售前专属会话ID
    private Date sendTime; // 消息发送时间
    private Integer isRead; // 是否已读
    private Date createTime; // 创建时间

    // 常量复用PO类的定义，避免重复
    public static final String SENDER_TYPE_USER = PreSaleChatMessagePO.SENDER_TYPE_USER;
    public static final String SENDER_TYPE_CS = PreSaleChatMessagePO.SENDER_TYPE_CS;
    public static final String SENDER_TYPE_SYSTEM = PreSaleChatMessagePO.SENDER_TYPE_SYSTEM;

    public static final String MSG_TYPE_TEXT = PreSaleChatMessagePO.MSG_TYPE_TEXT;
    public static final String MSG_TYPE_SYSTEM_TIP = PreSaleChatMessagePO.MSG_TYPE_SYSTEM_TIP;

    public static final Integer IS_READ_NO = PreSaleChatMessagePO.IS_READ_NO;
    public static final Integer IS_READ_YES = PreSaleChatMessagePO.IS_READ_YES;
}