package com.yqrb.handler;

import io.netty.util.AttributeKey;

/**
 * WebSocket Channel 属性键（存储用户参数，纯内存存储）
 */
public class WebSocketAttrKey {
    /** 用户ID */
    public static final AttributeKey<String> USER_ID = AttributeKey.newInstance("userId");

    /** 用户类型（USER/CUSTOMER_SERVICE） */
    public static final AttributeKey<String> USER_TYPE = AttributeKey.newInstance("userType");
}