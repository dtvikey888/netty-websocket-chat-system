package com.yqrb.netty.constant;

import io.netty.util.AttributeKey;

public class NettyConstant {
    // ====================== 售后专属常量（原有，不修改）======================
    // 通道属性Key - 严格绑定【业务会话ID】（SESSION_xxx），仅用于标识聊天会话
    public static final AttributeKey<String> SESSION_ID_KEY = AttributeKey.valueOf("SESSION_ID");
    // 通道属性Key - 严格绑定【连接者/发送者ID】（LYQY_USER_xxx/LYQY_CS_xxx），仅用于标识谁的连接/谁发的消息
    public static final AttributeKey<String> RECEIVER_ID_KEY = AttributeKey.valueOf("RECEIVER_ID");
    // 通道属性Key - 发送者类型（USER/CS/SYSTEM）
    public static final AttributeKey<String> SENDER_TYPE_KEY = AttributeKey.valueOf("SENDER_TYPE");


    // 其他公共常量可在此补充
    // ====================== 售前专属常量（新增，核心隔离）======================
    public static final AttributeKey<String> PRE_SALE_SESSION_ID_KEY = AttributeKey.valueOf("PRE_SALE_SESSION_ID");
    public static final AttributeKey<String> PRE_SALE_RECEIVER_ID_KEY = AttributeKey.valueOf("PRE_SALE_RECEIVER_ID");
    public static final AttributeKey<String> PRE_SALE_SENDER_TYPE_KEY = AttributeKey.valueOf("PRE_SALE_SENDER_TYPE");
    public static final AttributeKey<String> PRE_SALE_USER_ID_KEY = AttributeKey.valueOf("PRE_SALE_USER_ID");

    // 公共常量
    // 通道属性Key - 消息接收者ID（仅临时用，无需保留）
    public static final AttributeKey<String> USER_ID_KEY = AttributeKey.valueOf("USER_ID");
}