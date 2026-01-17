package com.yqrb.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yqrb.dto.ChatMessageDTO;
import com.yqrb.enums.ApplicationStatusEnum;
import com.yqrb.enums.MessageTypeEnum;
import com.yqrb.enums.UserTypeEnum;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 纯内存数据管理工具类（线程安全）
 * 管理：WebSocket 通道、聊天历史、登报申请、用户与客服绑定关系
 */
@Slf4j
@Component
public class WebSocketMemoryManager {
    // ==================== 全局配置 ====================
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final AtomicInteger APP_COUNTER = new AtomicInteger(10000); // 申请ID计数器
    private static final AtomicInteger SESSION_COUNTER = new AtomicInteger(0); // 会话ID计数器

    // ==================== 纯内存存储集合（核心，所有数据均存在此处） ====================
    /** 用户ID -> 通道信息（用户/客服） */
    private final Map<String, ChannelInfo> USER_CHANNEL_MAP = new ConcurrentHashMap<>();
    /** 用户ID -> 绑定的客服ID（普通用户专属） */
    private final Map<String, String> USER_CS_BIND_MAP = new ConcurrentHashMap<>();
    /** 客服ID -> 绑定的用户ID列表（客服专属） */
    private final Map<String, List<String>> CS_USER_BIND_MAP = new ConcurrentHashMap<>();
    /** 聊天历史记录（用户ID -> 消息列表） */
    private final Map<String, List<ChatMessageDTO>> CHAT_HISTORY_MAP = new ConcurrentHashMap<>();
    /** 登报申请列表（申请ID -> 申请信息） */
    private final Map<String, ApplicationInfo> APPLICATION_MAP = new ConcurrentHashMap<>();

    // ==================== 初始化模拟数据（Spring 单例 Bean 创建后自动执行，用于联调） ====================
    /**
     * 替换 static 代码块，使用 @PostConstruct 注解，Spring 单例 Bean 初始化后执行
     * 模拟数据存入 Spring 单例的 APPLICATION_MAP，业务接口可查询到
     */
    @PostConstruct
    public void initMockApplications() {
        // 直接调用当前实例的方法，数据存入当前实例（Spring 单例）的 APPLICATION_MAP
        createMockApplication("user001", "张三", "13800138000", "营业执照遗失登报");
        createMockApplication("user002", "李四", "13900139000", "公章遗失登报");
        log.info("纯内存模拟数据初始化完成，共添加 {} 条待审核登报申请", APPLICATION_MAP.size());
    }

    /**
     * 生成模拟登报申请（纯内存，无数据库）
     */
    private void createMockApplication(String userId, String userName, String userPhone, String applyType) {
        String appId = "APP_" + System.currentTimeMillis() + "_" + APP_COUNTER.incrementAndGet();
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.setAppId(appId);
        appInfo.setUserId(userId);
        appInfo.setUserName(userName);
        appInfo.setUserPhone(userPhone);
        appInfo.setApplyType(applyType);
        appInfo.setSubmitTime(LocalDateTime.now().minusHours(1).toString());
        appInfo.setStatus(ApplicationStatusEnum.PENDING);
        APPLICATION_MAP.put(appId, appInfo);
    }

    // ==================== WebSocket 通道管理 ====================
    /**
     * 添加用户通道会话（纯内存）
     */
    public void addChannelSession(String userId, UserTypeEnum userType, Channel channel) {
        if (userId == null || userType == null || channel == null) {
            return;
        }
        USER_CHANNEL_MAP.put(userId, new ChannelInfo(userId, userType, channel, LocalDateTime.now()));
        // 客服用户，初始化绑定用户列表
        if (UserTypeEnum.CUSTOMER_SERVICE.equals(userType) && !CS_USER_BIND_MAP.containsKey(userId)) {
            CS_USER_BIND_MAP.put(userId, new ArrayList<>());
        }
    }

    /**
     * 移除用户通道会话（纯内存清理）
     */
    public void removeChannelSession(String userId) {
        if (userId == null) {
            return;
        }
        USER_CHANNEL_MAP.remove(userId);
    }

    /**
     * 判断用户是否在线（纯内存查询）
     */
    public boolean isUserOnline(String userId) {
        ChannelInfo channelInfo = USER_CHANNEL_MAP.get(userId);
        return channelInfo != null && channelInfo.getChannel().isActive();
    }

    /**
     * 获取用户类型（纯内存查询）
     */
    public UserTypeEnum getUserType(String userId) {
        ChannelInfo channelInfo = USER_CHANNEL_MAP.get(userId);
        return channelInfo != null ? channelInfo.getUserType() : null;
    }

    // ==================== 消息推送与历史记录 ====================
    /**
     * 定向发送消息给指定用户（纯内存）
     */
    public void sendMessageToUser(String userId, ChatMessageDTO message) {
        if (userId == null || message == null) {
            log.warn("定向发送消息失败：参数为空");
            return;
        }
        ChannelInfo channelInfo = USER_CHANNEL_MAP.get(userId);
        if (channelInfo == null || !channelInfo.getChannel().isActive()) {
            log.warn("用户[{}]连接不存在或已断开，无法发送消息", userId);
            return;
        }
        sendMessage(channelInfo.getChannel(), message);
    }

    /**
     * 保存聊天历史记录（纯内存）
     */
    public void saveChatHistory(ChatMessageDTO message) {
        if (message == null || message.getSenderId() == null) {
            return;
        }
        // 保存到发送方历史
        CHAT_HISTORY_MAP.computeIfAbsent(message.getSenderId(), k -> new ArrayList<>()).add(message);
        // 保存到接收方历史
        if (message.getReceiverId() != null) {
            CHAT_HISTORY_MAP.computeIfAbsent(message.getReceiverId(), k -> new ArrayList<>()).add(message);
        }
    }

    /**
     * 获取用户聊天历史记录（纯内存）
     */
    public List<ChatMessageDTO> getChatHistory(String userId) {
        return CHAT_HISTORY_MAP.getOrDefault(userId, new ArrayList<>());
    }

    /**
     * 广播消息给绑定该客服的所有用户（纯内存）
     */
    public void broadcastToBindUsers(String csId, ChatMessageDTO message) {
        List<String> bindUsers = CS_USER_BIND_MAP.getOrDefault(csId, new ArrayList<>());
        bindUsers.forEach(userId -> sendMessageToUser(userId, message));
    }

    /**
     * 消息序列化（转为 JSON 字符串，纯内存操作）
     */
    private void sendMessage(Channel channel, ChatMessageDTO message) {
        try {
            String jsonMessage = OBJECT_MAPPER.writeValueAsString(message);
            channel.writeAndFlush(new TextWebSocketFrame(jsonMessage));
            log.debug("消息发送成功（纯内存）：{}", message.getMsgId());
        } catch (JsonProcessingException e) {
            log.error("消息序列化失败", e);
        }
    }

    // ==================== 登报申请管理（纯内存，无数据库） ====================
    /**
     * 获取所有待审核申请
     */
    public List<ApplicationInfo> getPendingApplications() {
        List<ApplicationInfo> pendingList = new ArrayList<>();
        for (ApplicationInfo appInfo : APPLICATION_MAP.values()) {
            if (ApplicationStatusEnum.PENDING.equals(appInfo.getStatus())) {
                pendingList.add(appInfo);
            }
        }
        return pendingList;
    }

    /**
     * 根据申请ID获取申请信息
     */
    public ApplicationInfo getApplicationInfo(String appId) {
        return APPLICATION_MAP.get(appId);
    }

    /**
     * 审核通过（更新状态、设置金额、推送付款提醒）
     */
    public String handleAuditPass(String appId, String userId, BigDecimal payAmount) {
        ApplicationInfo appInfo = APPLICATION_MAP.get(appId);
        if (appInfo == null) {
            throw new RuntimeException("申请不存在（纯内存）");
        }

        // 1. 纯内存更新申请状态
        appInfo.setStatus(ApplicationStatusEnum.PASS);
        appInfo.setPayAmount(payAmount.toString());
        appInfo.setAuditTime(LocalDateTime.now().toString());

        // 2. 生成订单编号（纯内存，无数据库）
        String orderNo = "ORDER_" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
        appInfo.setOrderNo(orderNo);

        // 3. 构造付款提醒消息
        ChatMessageDTO payRemindMsg = new ChatMessageDTO();
        payRemindMsg.setMsgId(UUID.randomUUID().toString());
        payRemindMsg.setSenderId("system");
        payRemindMsg.setReceiverId(userId);
        payRemindMsg.setContent(String.format("【登报申请审核通过】\n您的登报申请（ID：%s）已审核通过，请尽快完成付款！\n应付金额：¥%.2f\n订单编号：%s\n温馨提示：付款后工作人员将告知刊登信息",
                appId, payAmount, orderNo));
        payRemindMsg.setMsgType(MessageTypeEnum.PAY_REMIND);
        payRemindMsg.setSendTime(LocalDateTime.now());

        // 4. 推送消息
        sendMessageToUser(userId, payRemindMsg);
        saveChatHistory(payRemindMsg);

        log.info("付款提醒已推送（纯内存）：用户[{}]，订单[{}]", userId, orderNo);
        return orderNo;
    }

    /**
     * 审核驳回（更新状态、设置原因、推送驳回提醒）
     */
    public void handleAuditReject(String appId, String userId, String rejectReason) {
        ApplicationInfo appInfo = APPLICATION_MAP.get(appId);
        if (appInfo == null) {
            throw new RuntimeException("申请不存在（纯内存）");
        }

        // 1. 纯内存更新申请状态
        appInfo.setStatus(ApplicationStatusEnum.REJECT);
        appInfo.setRejectReason(rejectReason);
        appInfo.setAuditTime(LocalDateTime.now().toString());

        // 2. 构造驳回提醒消息
        ChatMessageDTO rejectMsg = new ChatMessageDTO();
        rejectMsg.setMsgId(UUID.randomUUID().toString());
        rejectMsg.setSenderId("system");
        rejectMsg.setReceiverId(userId);
        rejectMsg.setContent(String.format("【登报申请驳回通知】\n您的登报申请（ID：%s）已被驳回\n驳回原因：%s\n温馨提示：修改资料后可重新提交",
                appId, rejectReason));
        rejectMsg.setMsgType(MessageTypeEnum.REJECT_REMIND);
        rejectMsg.setSendTime(LocalDateTime.now());

        // 3. 推送消息
        sendMessageToUser(userId, rejectMsg);
        saveChatHistory(rejectMsg);

        log.info("驳回提醒已推送（纯内存）：用户[{}]，申请[{}]", userId, appId);
    }

    // ==================== 用户与客服绑定关系管理（纯内存） ====================
    /**
     * 绑定用户与客服
     */
    public void bindUserAndCs(String userId, String csId) {
        USER_CS_BIND_MAP.put(userId, csId);
        CS_USER_BIND_MAP.computeIfAbsent(csId, k -> new ArrayList<>()).add(userId);
    }

    /**
     * 获取用户绑定的客服ID
     */
    public String getBindCsId(String userId) {
        return USER_CS_BIND_MAP.get(userId);
    }

    /**
     * 获取空闲客服ID（简单轮询）
     */
    public String getIdleCustomerServiceId() {
        for (Map.Entry<String, ChannelInfo> entry : USER_CHANNEL_MAP.entrySet()) {
            if (UserTypeEnum.CUSTOMER_SERVICE.equals(entry.getValue().getUserType())
                    && entry.getValue().getChannel().isActive()) {
                return entry.getKey();
            }
        }
        return null;
    }

    // ==================== 内部实体类（纯内存，无数据库对应） ====================
    /**
     * 通道信息实体（纯内存存储）
     */
    @lombok.Data
    private static class ChannelInfo {
        private String userId;
        private UserTypeEnum userType;
        private Channel channel;
        private LocalDateTime connectTime;

        public ChannelInfo(String userId, UserTypeEnum userType, Channel channel, LocalDateTime connectTime) {
            this.userId = userId;
            this.userType = userType;
            this.channel = channel;
            this.connectTime = connectTime;
        }
    }

    /**
     * 登报申请信息实体（纯内存存储，无数据库对应）
     */
    @lombok.Data
    public static class ApplicationInfo {
        private String appId;
        private String userId;
        private String userName;
        private String userPhone;
        private String applyType;
        private String submitTime;
        private ApplicationStatusEnum status;
        private String payAmount;
        private String orderNo;
        private String rejectReason;
        private String auditTime;
        private String auditRemark;
    }
}