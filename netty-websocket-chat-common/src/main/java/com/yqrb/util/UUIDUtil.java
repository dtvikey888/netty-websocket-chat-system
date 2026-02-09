package com.yqrb.util;

import cn.hutool.core.util.IdUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * 唯一 ID 生成工具
 */
public class UUIDUtil {

    // 常量定义（统一管理前缀，便于维护）
    private static final String PRE_SESSION_PREFIX = "PRE_SESSION_";
    private static final String PRE_MSG_PREFIX = "PRE_MSG_";
    private static final String MSG_PREFIX = "MSG_";
    private static final String SESSION_PREFIX = "SESSION_";
    private static final String APP_PREFIX = "APP_";
    private static final String FILE_PREFIX = "FILE_";

    // 生成无横线UUID
    public static String getUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // 生成登报申请ID
    public static String generateAppId() {
        return APP_PREFIX + getUUID();
    }

    // 生成会话ID
    public static String generateSessionId() {
        return SESSION_PREFIX + getUUID();
    }

    // 生成普通消息ID（原有逻辑，保持不变）
    public static String generateMsgId() {
        return MSG_PREFIX + getUUID();
    }

    // 生成文件ID
    public static String generateFileId() {
        return FILE_PREFIX + getUUID();
    }

    // 原有生成方法（保留，兼容其他业务）
    public static String generateUUID() {
        return IdUtil.fastUUID().replace("-", "");
    }

    // ========== 售前相关ID生成（核心新增） ==========
    /**
     * 生成售前会话ID（格式：PRE_SESSION_+无横线UUID）
     * 参考generateSessionId风格，统一前缀管理
     */
    public static String generatePreSaleSessionId() {
        return PRE_SESSION_PREFIX + getUUID();
    }

    /**
     * 生成售前消息ID（格式：PRE_MSG_+时间戳+_+会话ID后缀）
     * @param preSaleSessionId 售前会话ID（格式：PRE_SESSION_+UUID）
     * @return 符合规范的售前消息ID
     */
    public static String generatePreSaleMsgId(String preSaleSessionId) {
        // 1. 获取毫秒级时间戳
        long timestamp = System.currentTimeMillis();
        // 2. 提取会话ID后缀（兼容异常场景）
        String sessionSuffix = "";
        if (StringUtils.isNotBlank(preSaleSessionId) && preSaleSessionId.startsWith(PRE_SESSION_PREFIX)) {
            sessionSuffix = preSaleSessionId.substring(PRE_SESSION_PREFIX.length());
            // 截取后8位简化（可选，保持和原有逻辑一致）
            if (sessionSuffix.length() > 8) {
                sessionSuffix = sessionSuffix.substring(sessionSuffix.length() - 8);
            }
        } else {
            // 会话ID异常时，用8位随机UUID兜底
            sessionSuffix = getUUID().substring(0, 8);
        }
        // 3. 拼接最终格式（PRE_MSG_+时间戳+_+后缀）
        return PRE_MSG_PREFIX + timestamp + "_" + sessionSuffix;
    }

    // ========== 乐音清扬用户专属ReceiverId生成（原有逻辑保留） ==========
    /**
     * 生成乐音清扬用户固定ReceiverId（无随机串）
     * @param userType 用户类型（USER/ADMIN/CS）
     * @param appId 用户专属appId（如001、002、abc123等）
     * @return 固定ReceiverId（格式：R_FIXED_0000_LYQY_<userType>_<appId>）
     */
    public static String generateLyqyFixedReceiverId(String userType, String appId) {
        if (appId == null || appId.trim().isEmpty()) {
            throw new IllegalArgumentException("乐音清扬用户专属appId不能为空！");
        }
        if (userType == null || (!"USER".equals(userType) && !"ADMIN".equals(userType) && !"CS".equals(userType))) {
            throw new IllegalArgumentException("用户类型仅支持USER/ADMIN/CS！");
        }
        return "R_FIXED_0000_LYQY_" + userType + "_" + appId.trim();
    }

    // 兼容原有生成方法（非乐音清扬用户仍用随机规则）
    public static String generateReceiverId(String userId) {
        return "RECEIVER_" + userId + "_" + generateUUID();
    }

    // 测试示例
    public static void main(String[] args) {
        // 测试售前会话ID生成
        String preSessionId = generatePreSaleSessionId();
        System.out.println("售前会话ID：" + preSessionId);

        // 测试售前消息ID生成
        String preMsgId = generatePreSaleMsgId(preSessionId);
        System.out.println("售前消息ID：" + preMsgId); // 示例输出：PRE_MSG_1770102905854_12345678

        // 原有方法测试
        System.out.println("普通消息ID：" + generateMsgId()); // 示例输出：MSG_6f2a65b1f62e4aacad0eacb18bb8f231
    }
}