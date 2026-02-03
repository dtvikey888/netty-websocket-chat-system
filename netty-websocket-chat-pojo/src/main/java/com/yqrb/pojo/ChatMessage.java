package com.yqrb.pojo;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Table(name = "chat_message")
public class ChatMessage {
    /**
     * 主键ID
     */
    @Id
    private Long id;

    /**
     * 消息唯一标识
     */
    @Column(name = "msg_id")
    private String msgId;

    /**
     * 发送者ID（对应receiverId格式）
     */
    @Column(name = "sender_id")
    private String senderId;

    /**
     * 发送者类型：USER-用户，CS-客服，SYSTEM-系统
     */
    @Column(name = "sender_type")
    private String senderType;

    /**
     * 接收者ID（核心，闭环流转字段）
     */
    @Column(name = "receiver_id")
    private String receiverId;

    /**
     * 附件路径（多张用逗号分隔，存储对象存储地址）
     */
    @Column(name = "attachment_path")
    private String attachmentPath;

    /**
     * 消息类型：TEXT-文本，PAY_REMIND-付款提醒，SYSTEM_TIP-系统提示
     */
    @Column(name = "msg_type")
    private String msgType;

    /**
     * 会话唯一标识（关联session_mapping表的session_id，用于绑定登报申请app_id）
     */
    @Column(name = "session_id")
    private String sessionId;

    /**
     * 发送时间
     */
    @Column(name = "send_time")
    private Date sendTime;

    /**
     * 是否已读：0-未读，1-已读
     */
    @Column(name = "is_read")
    private Boolean isRead;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private Date createTime;

    /**
     * 消息内容
     */
    private String content;

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
     * 获取消息唯一标识
     *
     * @return msg_id - 消息唯一标识
     */
    public String getMsgId() {
        return msgId;
    }

    /**
     * 设置消息唯一标识
     *
     * @param msgId 消息唯一标识
     */
    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    /**
     * 获取发送者ID（对应receiverId格式）
     *
     * @return sender_id - 发送者ID（对应receiverId格式）
     */
    public String getSenderId() {
        return senderId;
    }

    /**
     * 设置发送者ID（对应receiverId格式）
     *
     * @param senderId 发送者ID（对应receiverId格式）
     */
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    /**
     * 获取发送者类型：USER-用户，CS-客服，SYSTEM-系统
     *
     * @return sender_type - 发送者类型：USER-用户，CS-客服，SYSTEM-系统
     */
    public String getSenderType() {
        return senderType;
    }

    /**
     * 设置发送者类型：USER-用户，CS-客服，SYSTEM-系统
     *
     * @param senderType 发送者类型：USER-用户，CS-客服，SYSTEM-系统
     */
    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    /**
     * 获取接收者ID（核心，闭环流转字段）
     *
     * @return receiver_id - 接收者ID（核心，闭环流转字段）
     */
    public String getReceiverId() {
        return receiverId;
    }

    /**
     * 设置接收者ID（核心，闭环流转字段）
     *
     * @param receiverId 接收者ID（核心，闭环流转字段）
     */
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    /**
     * 获取附件路径（多张用逗号分隔，存储对象存储地址）
     *
     * @return attachment_path - 附件路径（多张用逗号分隔，存储对象存储地址）
     */
    public String getAttachmentPath() {
        return attachmentPath;
    }

    /**
     * 设置附件路径（多张用逗号分隔，存储对象存储地址）
     *
     * @param attachmentPath 附件路径（多张用逗号分隔，存储对象存储地址）
     */
    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }

    /**
     * 获取消息类型：TEXT-文本，PAY_REMIND-付款提醒，SYSTEM_TIP-系统提示
     *
     * @return msg_type - 消息类型：TEXT-文本，PAY_REMIND-付款提醒，SYSTEM_TIP-系统提示
     */
    public String getMsgType() {
        return msgType;
    }

    /**
     * 设置消息类型：TEXT-文本，PAY_REMIND-付款提醒，SYSTEM_TIP-系统提示
     *
     * @param msgType 消息类型：TEXT-文本，PAY_REMIND-付款提醒，SYSTEM_TIP-系统提示
     */
    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    /**
     * 获取会话唯一标识（关联session_mapping表的session_id，用于绑定登报申请app_id）
     *
     * @return session_id - 会话唯一标识（关联session_mapping表的session_id，用于绑定登报申请app_id）
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话唯一标识（关联session_mapping表的session_id，用于绑定登报申请app_id）
     *
     * @param sessionId 会话唯一标识（关联session_mapping表的session_id，用于绑定登报申请app_id）
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 获取发送时间
     *
     * @return send_time - 发送时间
     */
    public Date getSendTime() {
        return sendTime;
    }

    /**
     * 设置发送时间
     *
     * @param sendTime 发送时间
     */
    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }

    /**
     * 获取是否已读：0-未读，1-已读
     *
     * @return is_read - 是否已读：0-未读，1-已读
     */
    public Boolean getIsRead() {
        return isRead;
    }

    /**
     * 设置是否已读：0-未读，1-已读
     *
     * @param isRead 是否已读：0-未读，1-已读
     */
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
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
     * 获取消息内容
     *
     * @return content - 消息内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置消息内容
     *
     * @param content 消息内容
     */
    public void setContent(String content) {
        this.content = content;
    }
}