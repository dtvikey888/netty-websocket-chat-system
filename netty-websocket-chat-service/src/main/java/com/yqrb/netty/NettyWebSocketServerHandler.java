package com.yqrb.netty;

import com.yqrb.pojo.vo.WebSocketMsgVO;
import com.yqrb.service.ReceiverIdService;
import com.yqrb.util.DateUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Netty WebSocket消息处理器（处理连接、断开、消息转发）
 */
@Component
public class NettyWebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketMsgVO> {

    // 保存所有在线通道（全局共享）
    private static final ChannelGroup ONLINE_CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    // 保存ReceiverId与Channel的映射关系
    private static final Map<String, Channel> RECEIVER_CHANNEL_MAP = new ConcurrentHashMap<>();

    // 注入ReceiverId服务（解决Netty Handler无法直接注入Spring Bean问题）
    private static ReceiverIdService receiverIdService;

    @Autowired
    private ReceiverIdService receiverIdServiceAutowire;

    @PostConstruct
    public void init() {
        NettyWebSocketServerHandler.receiverIdService = this.receiverIdServiceAutowire;
    }

    /**
     * 客户端连接建立成功时触发
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        ONLINE_CHANNELS.add(channel);
        System.out.println("新客户端连接：" + channel.id().asShortText() + "，当前在线人数：" + ONLINE_CHANNELS.size());
    }

    /**
     * 客户端连接断开时触发
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        ONLINE_CHANNELS.remove(channel);

        // 移除ReceiverId与Channel的映射，并标记用户离线
        RECEIVER_CHANNEL_MAP.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(channel)) {
                String receiverId = entry.getKey();
                receiverIdService.markOffline(receiverId);
                System.out.println("客户端离线：" + receiverId + "，通道：" + channel.id().asShortText());
                return true;
            }
            return false;
        });

        System.out.println("客户端断开连接：" + channel.id().asShortText() + "，当前在线人数：" + ONLINE_CHANNELS.size());
    }

    /**
     * 处理空闲超时事件（客户端长时间无交互，断开连接）
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleEvent = (IdleStateEvent) evt;
            if (idleEvent.state() == IdleState.READER_IDLE) {
                Channel channel = ctx.channel();
                System.out.println("客户端空闲超时，断开连接：" + channel.id().asShortText());
                channel.close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    /**
     * 处理接收到的客户端消息
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketMsgVO webSocketMsg) throws Exception {
        Channel currentChannel = ctx.channel();
        String receiverId = webSocketMsg.getReceiverId();

        // 1. 校验ReceiverId有效性，首次消息绑定Channel与ReceiverId
        if (!RECEIVER_CHANNEL_MAP.containsKey(receiverId)) {
            if (!receiverIdService.validateReceiverId(receiverId)) {
                System.out.println("无效的ReceiverId，拒绝消息：" + receiverId);
                currentChannel.close();
                return;
            }
            // 绑定ReceiverId与Channel，并标记用户在线
            RECEIVER_CHANNEL_MAP.put(receiverId, currentChannel);
            receiverIdService.markOnline(receiverId);
            System.out.println("绑定ReceiverId与通道：" + receiverId + " -> " + currentChannel.id().asShortText());
        }

        // 2. 补全消息默认信息
        if (webSocketMsg.getSendTime() == null) {
            webSocketMsg.setSendTime(new Date());
        }
        if (webSocketMsg.getMsgType() == null) {
            webSocketMsg.setMsgType("TEXT");
        }

        // 3. 转发消息（此处简化：直接发送给目标接收者，实际可扩展会话转发逻辑）
        forwardMessage(webSocketMsg);

        System.out.println("处理客户端消息：" + DateUtil.formatDate(webSocketMsg.getSendTime(), "yyyy-MM-dd HH:mm:ss")
                + "，发送者：" + webSocketMsg.getUserId() + "，内容：" + webSocketMsg.getMsgContent());
    }

    /**
     * 处理通道异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        System.out.println("客户端通道异常：" + channel.id().asShortText() + "，异常信息：" + cause.getMessage());
        channel.close();
    }

    /**
     * 转发消息到目标接收者
     * @param webSocketMsg 待转发的消息
     */
    private void forwardMessage(WebSocketMsgVO webSocketMsg) {
        String targetReceiverId = webSocketMsg.getReceiverId();
        Channel targetChannel = RECEIVER_CHANNEL_MAP.get(targetReceiverId);

        if (targetChannel != null && targetChannel.isActive()) {
            // 目标在线，直接发送
            targetChannel.writeAndFlush(webSocketMsg);
        } else {
            // 目标离线，可扩展：保存离线消息到Redis
            System.out.println("目标接收者离线，暂存离线消息：" + targetReceiverId);
            // 此处可集成RedisUtil，将消息存入离线消息队列
        }
    }
}