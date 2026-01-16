package com.yqrb.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yqrb.dto.ChatMessageDTO;
import com.yqrb.enums.MessageTypeEnum;
import com.yqrb.enums.UserTypeEnum;
import com.yqrb.utils.WebSocketMemoryManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 自定义 WebSocket 业务处理器（处理文本消息，纯内存操作，无数据库依赖）
 */
@Slf4j
@Component
public class CustomWebSocketBusinessHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    @Autowired
    private WebSocketMemoryManager webSocketMemoryManager;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ==================== 连接建立事件 ====================
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        // 从 Channel 属性中获取握手时传递的用户参数（用户ID、用户类型）
        String userId = ctx.channel().attr(WebSocketAttrKey.USER_ID).get();
        String userTypeStr = ctx.channel().attr(WebSocketAttrKey.USER_TYPE).get();

        if (userId == null || userTypeStr == null) {
            ctx.close();
            log.warn("连接建立失败：缺少用户参数（纯内存存储，无法从数据库补全）");
            return;
        }

        try {
            UserTypeEnum userType = UserTypeEnum.valueOf(userTypeStr);
            // 纯内存：添加连接会话到内存管理工具类
            webSocketMemoryManager.addChannelSession(userId, userType, ctx.channel());
            log.info("用户[{}]（类型：{}）WebSocket 连接建立成功（纯内存存储）", userId, userType);

            // 发送连接成功通知
            sendConnectSuccessMsg(userId, userType);
        } catch (IllegalArgumentException e) {
            ctx.close();
            log.warn("连接建立失败：用户类型不合法 - {}", userTypeStr);
        }
    }

    // ==================== 消息接收事件（核心：处理前端发送的消息） ====================
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        // 1. 解析前端发送的 JSON 消息
        String jsonMsg = msg.text();
        log.debug("收到前端消息（纯内存处理）：{}", jsonMsg);

        ChatMessageDTO chatMessage;
        try {
            chatMessage = OBJECT_MAPPER.readValue(jsonMsg, ChatMessageDTO.class);
        } catch (Exception e) {
            log.error("消息解析失败：{}", jsonMsg, e);
            sendSystemTipMsg(ctx.channel().attr(WebSocketAttrKey.USER_ID).get(), "消息格式错误，无法解析");
            return;
        }

        // 2. 补全消息属性（纯内存生成，无数据库关联）
        if (chatMessage.getMsgId() == null || chatMessage.getMsgId().trim().isEmpty()) {
            chatMessage.setMsgId(UUID.randomUUID().toString());
        }
        chatMessage.setSendTime(LocalDateTime.now());
        if (chatMessage.getMsgType() == null) {
            chatMessage.setMsgType(MessageTypeEnum.TEXT);
        }

        // 3. 纯内存：保存聊天消息到历史记录（供后续查询）
        webSocketMemoryManager.saveChatHistory(chatMessage);

        // 4. 定向转发消息（用户 ↔ 客服）
        String receiverId = chatMessage.getReceiverId();
        if (receiverId == null || receiverId.trim().isEmpty()) {
            log.warn("消息转发失败：接收方ID为空");
            sendSystemTipMsg(chatMessage.getSenderId(), "接收方不存在，无法发送消息");
            return;
        }

        if (webSocketMemoryManager.isUserOnline(receiverId)) {
            // 转发消息给接收方
            webSocketMemoryManager.sendMessageToUser(receiverId, chatMessage);
            // 回执消息给发送方（确认消息已发送）
            webSocketMemoryManager.sendMessageToUser(chatMessage.getSenderId(), chatMessage);
            log.info("消息转发成功（纯内存）：{} -> {}", chatMessage.getSenderId(), receiverId);
        } else {
            // 接收方不在线，发送系统提示
            sendSystemTipMsg(chatMessage.getSenderId(), "对方已下线，无法接收消息");
        }
    }

    // ==================== 连接断开事件 ====================
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        // 获取用户ID，清理内存中的连接会话
        String userId = ctx.channel().attr(WebSocketAttrKey.USER_ID).get();
        if (userId != null) {
            UserTypeEnum userType = webSocketMemoryManager.getUserType(userId);
            webSocketMemoryManager.removeChannelSession(userId);
            log.info("用户[{}] WebSocket 连接断开（纯内存清理完成）", userId);

            // 纯内存：发送下线通知给关联对象
            sendUserOfflineMsg(userId, userType);
        }
    }

    // ==================== 异常处理事件 ====================
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("WebSocket 连接异常（纯内存处理）", cause);
        // 清理内存中的连接会话
        String userId = ctx.channel().attr(WebSocketAttrKey.USER_ID).get();
        webSocketMemoryManager.removeChannelSession(userId);
        // 关闭通道
        ctx.close();
    }

    // ==================== 辅助方法：发送连接成功通知 ====================
    private void sendConnectSuccessMsg(String userId, UserTypeEnum userType) {
        ChatMessageDTO successMsg = new ChatMessageDTO();
        successMsg.setMsgId(UUID.randomUUID().toString());
        successMsg.setSenderId("system");
        successMsg.setReceiverId(userId);
        successMsg.setMsgType(MessageTypeEnum.CONNECT_SUCCESS);
        successMsg.setSendTime(LocalDateTime.now());

        if (UserTypeEnum.USER.equals(userType)) {
            // 纯内存：分配空闲客服
            String csId = webSocketMemoryManager.getIdleCustomerServiceId();
            if (csId != null) {
                successMsg.setContent(String.format("已为您分配客服[%s]，开始咨询吧~", csId));
                // 绑定用户与客服的会话（纯内存存储）
                webSocketMemoryManager.bindUserAndCs(userId, csId);
            } else {
                successMsg.setContent("当前暂无在线客服，请稍后再试");
            }
        } else {
            successMsg.setContent("客服端连接成功，可处理用户申请和聊天");
        }

        webSocketMemoryManager.sendMessageToUser(userId, successMsg);
    }

    // ==================== 辅助方法：发送系统提示 ====================
    private void sendSystemTipMsg(String userId, String content) {
        if (userId == null || !webSocketMemoryManager.isUserOnline(userId)) {
            return;
        }

        ChatMessageDTO tipMsg = new ChatMessageDTO();
        tipMsg.setMsgId(UUID.randomUUID().toString());
        tipMsg.setSenderId("system");
        tipMsg.setReceiverId(userId);
        tipMsg.setContent(content);
        tipMsg.setMsgType(MessageTypeEnum.SYSTEM_TIP);
        tipMsg.setSendTime(LocalDateTime.now());

        webSocketMemoryManager.sendMessageToUser(userId, tipMsg);
    }

    // ==================== 辅助方法：发送用户下线通知 ====================
    private void sendUserOfflineMsg(String userId, UserTypeEnum userType) {
        ChatMessageDTO offlineMsg = new ChatMessageDTO();
        offlineMsg.setMsgId(UUID.randomUUID().toString());
        offlineMsg.setSenderId("system");
        offlineMsg.setMsgType(MessageTypeEnum.USER_OFFLINE);
        offlineMsg.setSendTime(LocalDateTime.now());

        if (UserTypeEnum.USER.equals(userType)) {
            // 通知绑定的客服用户下线
            String csId = webSocketMemoryManager.getBindCsId(userId);
            if (csId != null && webSocketMemoryManager.isUserOnline(csId)) {
                offlineMsg.setReceiverId(csId);
                offlineMsg.setContent("用户已下线");
                webSocketMemoryManager.sendMessageToUser(csId, offlineMsg);
            }
        } else {
            // 通知所有绑定该客服的用户客服下线
            offlineMsg.setContent("客服已下线，后续将为您重新分配");
            webSocketMemoryManager.broadcastToBindUsers(userId, offlineMsg);
        }
    }
}
