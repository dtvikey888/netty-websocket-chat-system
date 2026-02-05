package com.yqrb.common.constant;

public class RedisChatKeyConstants {
    /**
     * 未读消息数量缓存 Key 格式：chat:unread:${receiverId}
     * 说明：receiverId 为接收者ID（用户/客服），值为未读消息总数（整数）
     */
    public static final String CHAT_UNREAD_COUNT = "chat:unread:%s";

    /**
     * 构建未读消息数量缓存 Key
     * @param receiverId 接收者ID
     * @return 完整Redis Key
     */
    public static String buildChatUnreadCountKey(String receiverId) {
        return String.format(CHAT_UNREAD_COUNT, receiverId);
    }
}
