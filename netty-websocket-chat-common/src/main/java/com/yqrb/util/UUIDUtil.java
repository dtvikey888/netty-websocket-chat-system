package com.yqrb.util;

import java.util.UUID;

/**
 * 唯一 ID 生成工具
 */
public class UUIDUtil {

    // 生成无横线UUID
    public static String getUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // 生成ReceiverId（用户会话唯一标识）
    public static String generateReceiverId(String userId) {
        return "RECEIVER_" + userId + "_" + getUUID();
    }

    // 生成登报申请ID
    public static String generateAppId() {
        return "APP_" + getUUID();
    }

    // 生成会话ID
    public static String generateSessionId() {
        return "SESSION_" + getUUID();
    }

    // 生成消息ID
    public static String generateMsgId() {
        return "MSG_" + getUUID();
    }

    // 生成文件ID
    public static String generateFileId() {
        return "FILE_" + getUUID();
    }

    public static void main(String[] args) {
        System.out.println(generateFileId());
    }
}