package com.yqrb.netty;

import com.yqrb.netty.constant.NettyConstant;
import io.netty.channel.Channel;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * Netty WebSocket 工具类，提供通道查询、消息推送辅助方法
 */
@Component
public class NettyWebSocketUtil {
    /**
     * 根据 receiverId 获取对应的 WebSocket 通道
     */
    public Channel getChannelByReceiverId(String receiverId) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            return null;
        }
        return NettyWebSocketServerHandler.RECEIVER_CHANNEL_MAP.get(receiverId);
    }

    /**
     * 判断 receiverId 是否在线（有活跃通道）
     */
    public boolean isReceiverOnline(String receiverId) {
        Channel channel = getChannelByReceiverId(receiverId);
        return channel != null && channel.isActive() && channel.isWritable();
    }

    // ========== 新增：根据sessionId查找Netty通道 ==========
    public Channel getChannelBySessionId(String sessionId) {
        if (!org.springframework.util.StringUtils.hasText(sessionId)) {
            return null;
        }
        Map<String, Channel> channelMap = NettyWebSocketServerHandler.RECEIVER_CHANNEL_MAP;
        if (CollectionUtils.isEmpty(channelMap)) {
            return null;
        }
        for (Channel channel : channelMap.values()) {
            String bindSessionId = channel.attr(NettyConstant.SESSION_ID_KEY).get();
            if (sessionId.equals(bindSessionId)) {
                return channel;
            }
        }
        return null;
    }
}