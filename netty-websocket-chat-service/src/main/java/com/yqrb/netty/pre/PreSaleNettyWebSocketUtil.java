package com.yqrb.netty.pre;

import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

/**
 * 售前Netty WebSocket工具类（完全独立）
 */
@Component
public class PreSaleNettyWebSocketUtil {
    /**
     * 根据ReceiverId获取售前WebSocket通道
     */
    public Channel getPreSaleChannelByReceiverId(String receiverId) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            return null;
        }
        return PreSaleNettyWebSocketServerHandler.PRE_SALE_RECEIVER_CHANNEL_MAP.get(receiverId);
    }

    /**
     * 判断售前ReceiverId是否在线
     */
    public boolean isPreSaleReceiverOnline(String receiverId) {
        Channel channel = getPreSaleChannelByReceiverId(receiverId);
        return channel != null && channel.isActive() && channel.isWritable();
    }
}