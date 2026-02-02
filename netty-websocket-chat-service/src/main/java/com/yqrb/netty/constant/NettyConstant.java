package com.yqrb.netty.constant;

import io.netty.util.AttributeKey;

public class NettyConstant {
    // 通道属性Key
    public static final AttributeKey<String> SESSION_ID_KEY = AttributeKey.valueOf("SESSION_ID");
    public static final AttributeKey<String> RECEIVER_ID_KEY = AttributeKey.valueOf("RECEIVER_ID");
    public static final AttributeKey<String> SENDER_TYPE_KEY = AttributeKey.valueOf("SENDER_TYPE");
    public static final AttributeKey<String> USER_ID_KEY = AttributeKey.valueOf("USER_ID");

    // 其他公共常量可在此补充
}