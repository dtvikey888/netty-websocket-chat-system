package com.yqrb.dto;

import com.yqrb.enums.MessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * WebSocket 消息传输对象（前后端统一格式，纯内存传输）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 消息唯一标识 */
    private String msgId;

    /** 发送方ID（用户ID/客服ID/系统） */
    private String senderId;

    /** 接收方ID（用户ID/客服ID） */
    private String receiverId;

    /** 消息内容 */
    private String content;

    /** 消息类型 */
    private MessageTypeEnum msgType;

    /** 会话ID（绑定用户与客服的会话） */
    private String sessionId;

    /** 发送时间 */
    private LocalDateTime sendTime;
}
