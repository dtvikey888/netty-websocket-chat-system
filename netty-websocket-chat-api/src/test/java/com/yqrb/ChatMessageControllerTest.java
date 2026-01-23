package com.yqrb;

import org.junit.Test;

/**
 * 极简方案：生成乐音清扬3类用户的固定ReceiverId（无任何依赖，必生效）
 */
public class ChatMessageControllerTest {

    // 乐音清扬3类用户的APP ID（根据你的业务规则填写）
    private static final String LYQY_USER_APP_ID = "LYQY_USER_001";    // 普通用户
    private static final String LYQY_ADMIN_APP_ID = "LYQY_ADMIN_001";  // 管理员
    private static final String LYQY_CS_APP_ID = "LYQY_CS_001";        // 客服

    // 固定标识（替换原有的时间戳，保证ReceiverId不变）
    // 可根据你的业务习惯修改，比如用固定数字、角色编码等
    private static final String FIXED_IDENTIFIER = "FIXED_0000";

    /**
     * 生成固定的ReceiverId（无需启动Spring上下文，无需依赖任何Service）
     */
    @Test
    public void generateReceiverIdByAppId() {
        // 1. 生成普通用户ReceiverId（固定）
        String userReceiverId = generateFixedReceiverId(LYQY_USER_APP_ID, "乐音清扬普通用户");
        // 2. 生成管理员ReceiverId（固定）
        String adminReceiverId = generateFixedReceiverId(LYQY_ADMIN_APP_ID, "乐音清扬管理员");
        // 3. 生成客服ReceiverId（固定）
        String csReceiverId = generateFixedReceiverId(LYQY_CS_APP_ID, "乐音清扬客服");

        // 打印最终结果（直接复制到Apifox使用，永久不变）
        System.out.println("==================== 乐音清扬固定ReceiverId ====================");
        System.out.println("普通用户APP ID：" + LYQY_USER_APP_ID);
        System.out.println("普通用户ReceiverId：" + userReceiverId);
        System.out.println("------------------------------------------------------------");
        System.out.println("管理员APP ID：" + LYQY_ADMIN_APP_ID);
        System.out.println("管理员ReceiverId：" + adminReceiverId);
        System.out.println("------------------------------------------------------------");
        System.out.println("客服APP ID：" + LYQY_CS_APP_ID);
        System.out.println("客服ReceiverId：" + csReceiverId);
        System.out.println("============================================================");
    }

    /**
     * 核心：生成固定的ReceiverId
     * 规则：前缀R_ + 固定标识 + _ + APP ID（保证同一角色ID永久不变）
     */
    private String generateFixedReceiverId(String appId, String userName) {
        String receiverId = "R_" + FIXED_IDENTIFIER + "_" + appId;
        System.out.println("为[" + userName + "]生成固定ReceiverId：" + receiverId);
        return receiverId;
    }

    // 测试其他接口使用固定ReceiverId
    @Test
    public void testOtherApi() {
        String userReceiverId = generateFixedReceiverId(LYQY_USER_APP_ID, "乐音清扬普通用户");
        System.out.println("测试接口使用的固定ReceiverId：" + userReceiverId);
        // 这里可以添加其他接口的测试逻辑，直接用固定的ReceiverId
    }
}