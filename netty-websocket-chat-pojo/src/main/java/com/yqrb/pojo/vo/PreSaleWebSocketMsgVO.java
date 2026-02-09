package com.yqrb.pojo.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 售前WebSocket消息传输VO
 * 结构和售后WebSocketMsgVO完全一致，仅区分售前业务
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreSaleWebSocketMsgVO {
    // ========== 消息类型常量（和售后保持完全一致） ==========
    public static final String MSG_TYPE_NEW_APPLICATION = "NEW_APPLICATION_REMIND"; // 新申请提醒
    public static final String MSG_TYPE_TEXT = "TEXT";                               // 文本消息
    public static final String MSG_TYPE_PAY_REMIND = "PAY_REMIND";                   // 支付提醒
    public static final String MSG_TYPE_SYSTEM_TIP = "SYSTEM_TIP";                   // 系统提示

    // ========== 发送者类型常量（和售后保持完全一致） ==========
    public static final String SENDER_TYPE_USER = "USER";        // 客户
    public static final String SENDER_TYPE_CS = "CS";            // 售前客服
    public static final String SENDER_TYPE_SYSTEM = "SYSTEM";    // 系统

    // ========== 核心字段（和售后保持完全一致，含字段顺序） ==========
    private String receiverId; // 接收者会话ID（售前客服/用户的serviceStaffId/userId）
    private String userId;     // 发送者用户ID（系统推送时填SYSTEM）
    private String msgContent; // 消息内容
    private String msgType;    // 消息类型
    private String sessionId;  // 售前会话ID

    // 关键优化：复用售后的JSONField注解，统一日期序列化格式
    @JSONField(format = "yyyy-MM-dd HH:mm:ss", serialize = true, deserialize = true)
    private Date sendTime;     // 发送时间

    private String senderType; // 发送者类型：USER/CS/SYSTEM
}