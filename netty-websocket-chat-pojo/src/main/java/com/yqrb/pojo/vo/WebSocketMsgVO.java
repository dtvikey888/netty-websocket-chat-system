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
    // 消息类型常量 - 新增：新申请提醒
    public static final String MSG_TYPE_NEW_APPLICATION = "NEW_APPLICATION_REMIND";
    public static final String MSG_TYPE_TEXT = "TEXT";
    public static final String MSG_TYPE_PAY_REMIND = "PAY_REMIND";
    public static final String MSG_TYPE_SYSTEM_TIP = "SYSTEM_TIP";

    // 发送者类型常量
    public static final String SENDER_TYPE_USER = "USER";
    public static final String SENDER_TYPE_CS = "CS";
    public static final String SENDER_TYPE_SYSTEM = "SYSTEM";

    private String receiverId; // 接收者会话ID（客服/用户的serviceStaffId/userId）
    private String userId; // 发送者用户ID（系统推送时填SYSTEM）
    private String msgContent; // 消息内容
    private String msgType; // 消息类型
    private String sessionId; // 会话ID

    // 关键优化：指定Date字段的序列化格式，兼容字符串/时间戳解析
    @JSONField(format = "yyyy-MM-dd HH:mm:ss", serialize = true, deserialize = true)
    private Date sendTime; // 发送时间

    private String senderType; // 发送者类型：USER/CS/SYSTEM
}