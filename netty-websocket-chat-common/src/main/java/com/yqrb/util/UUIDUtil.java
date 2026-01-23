package com.yqrb.util;

import cn.hutool.core.util.IdUtil;

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
//    public static String generateReceiverId(String userId) {
//        return "RECEIVER_" + userId + "_" + getUUID();
//    }

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


    // 原有生成方法（保留，兼容其他业务）
    public static String generateUUID() {
        return IdUtil.fastUUID().replace("-", "");
    }



    // ========== 核心修改：支持乐音清扬用户专属appId的固定ReceiverId生成 ==========
    /**
     * 生成乐音清扬用户固定ReceiverId（无随机串）
     * @param userType 用户类型（USER/ADMIN/CS）
     * @param appId 用户专属appId（如001、002、abc123等）
     * @return 固定ReceiverId（格式：R_FIXED_0000_LYQY_<userType>_<appId>）
     */
    public static String generateLyqyFixedReceiverId(String userType, String appId) {
        // 校验参数，避免无效值
        if (appId == null || appId.trim().isEmpty()) {
            throw new IllegalArgumentException("乐音清扬用户专属appId不能为空！");
        }
        if (userType == null || (!"USER".equals(userType) && !"ADMIN".equals(userType) && !"CS".equals(userType))) {
            throw new IllegalArgumentException("用户类型仅支持USER/ADMIN/CS！");
        }
        // 固定规则：前缀 + 固定标识 + 乐音清扬前缀 + 用户类型 + 用户专属appId
        return "R_FIXED_0000_LYQY_" + userType + "_" + appId.trim();
    }

    // 兼容原有生成方法（非乐音清扬用户仍用随机规则）
    public static String generateReceiverId(String userId) {
        return "RECEIVER_" + userId + "_" + generateUUID();
    }

    public static void main(String[] args) {
        System.out.println(generateFileId());
    }
}