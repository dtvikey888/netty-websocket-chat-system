package com.yqrb.pojo.vo;

import com.alibaba.fastjson.annotation.JSONField;
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

    // 关键优化：指定Date字段的序列化格式，兼容字符串/时间戳解析
    @JSONField(format = "yyyy-MM-dd HH:mm:ss", serialize = true, deserialize = true)
    private Date sendTime; // 发送时间

    // 在 WebSocketMsgVO 类中添加以下字段（lombok 会自动生成 getter/setter）
    private String senderType; // 发送者类型：USER/CS/SYSTEM
}