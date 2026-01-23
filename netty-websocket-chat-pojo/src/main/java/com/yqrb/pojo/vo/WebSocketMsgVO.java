package com.yqrb.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMsgVO {
    private String receiverId; // 接收者会话ID
    private String userId; // 发送者用户ID
    private String msgContent; // 消息内容
    private String msgType; // 消息类型
    private String sessionId; // 会话ID
    private Date sendTime; // 发送时间
}