package com.yqrb.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yqrb.mapper.PreSaleChatMessageMapper;
import com.yqrb.netty.constant.NettyConstant;
import com.yqrb.netty.pre.PreSaleNettyWebSocketServerHandler;
import com.yqrb.pojo.po.PreSaleChatMessagePO;
import com.yqrb.pojo.vo.PreSaleChatMessageVO;
import com.yqrb.pojo.vo.PreSaleWebSocketMsgVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.pojo.vo.ResultCode;
import com.yqrb.service.PreSaleChatMessageService;
import com.yqrb.service.ReceiverIdService;
import com.yqrb.service.cache.PreSaleRedisUnreadMsgCacheService;
import com.yqrb.service.cache.RedisUnreadMsgCacheService;
import com.yqrb.util.UUIDUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 售前咨询聊天记录Service实现类（完整适配售后未读消息逻辑）
 */
@Service
public class PreSaleChatMessageServiceImpl implements PreSaleChatMessageService {
    private static final Logger logger = LoggerFactory.getLogger(PreSaleChatMessageServiceImpl.class);

    // 提取常量：统一管理前缀（和UUIDUtil风格对齐，便于维护）
    private static final String PRE_SESSION_PREFIX = "PRE_SESSION_";
    private static final String PRE_MSG_PREFIX = "PRE_MSG_";
    // 会话后缀截取长度
    private static final int SESSION_SUFFIX_LENGTH = 8;

    @Resource
    private PreSaleChatMessageMapper preSaleChatMessageMapper;

    @Resource
    private ReceiverIdService receiverIdService;

    // 新增：注入Redis未读消息缓存服务
    @Resource
    private PreSaleRedisUnreadMsgCacheService preSaleRedisUnreadMsgCacheService;


    @Override
    public Result<Void> wsReconnectPushUnread(String sessionId, String receiverId) {
        // 1. 参数校验：sessionId + receiverId 均非空
        if (!StringUtils.hasText(sessionId)) {
            return Result.paramError("sessionId不能为空");
        }
        if (!StringUtils.hasText(receiverId)) {
            return Result.paramError("receiverId不能为空");
        }

        try {
            // 处理ReceiverId前缀（和售后对齐：截取R_FIXED_0000_）
            String realReceiverId = receiverId.startsWith("R_FIXED_0000_")
                    ? receiverId.substring("R_FIXED_0000_".length())
                    : receiverId;

            // 2. 按sessionId + 真实ReceiverId 查询售前未读消息
            List<PreSaleChatMessagePO> unreadMsgPOList = preSaleChatMessageMapper.listUnreadBySessionIdAndReceiverId(sessionId, realReceiverId);
            if (CollectionUtils.isEmpty(unreadMsgPOList)) {
                // 无未读消息，自定义成功提示（泛型兼容Void）
                return Result.custom(ResultCode.SUCCESS, "售前重连成功，当前会话无未读消息");
            }

            // 3. 查找售前Netty通道（优先按真实ReceiverId，兜底按sessionId）
            Channel targetChannel = PreSaleNettyWebSocketServerHandler.PRE_SALE_RECEIVER_CHANNEL_MAP.get(realReceiverId);
            if (targetChannel == null) {
                targetChannel = findPreSaleChannelBySessionId(sessionId);
            }
            if (targetChannel == null || !targetChannel.isActive() || !targetChannel.isWritable()) {
                return Result.error("售前重连成功，但会话通道未在线，未读消息将在通道上线后自动推送");
            }

            // 4. 批量推送（适配售前WebSocketMsgVO）
            for (PreSaleChatMessagePO po : unreadMsgPOList) {
                PreSaleWebSocketMsgVO wsMsg = new PreSaleWebSocketMsgVO();
                wsMsg.setSessionId(po.getPreSaleSessionId());
                wsMsg.setReceiverId(po.getReceiverId()); // 限定售前接收方
                wsMsg.setUserId(po.getSenderId());
                wsMsg.setMsgContent(po.getContent());
                wsMsg.setMsgType(po.getMsgType());
                wsMsg.setSendTime(po.getSendTime());
                wsMsg.setSenderType(po.getSenderType());

                targetChannel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(wsMsg)));
            }

            logger.info("【售前WebSocket重连】sessionId：{}，receiverId：{}，成功推送{}条未读消息",
                    sessionId, receiverId, unreadMsgPOList.size());
            // 推送成功，自定义提示
            return Result.custom(ResultCode.SUCCESS, "售前重连成功，已推送" + unreadMsgPOList.size() + "条未读消息");

        } catch (Exception e) {
            logger.error("【售前WebSocket重连异常】sessionId：{}，receiverId：{}，异常信息：{}",
                    sessionId, receiverId, e.getMessage(), e);
            return Result.error("售前重连推送未读消息异常，请稍后重试");
        }
    }

    /**
     * 私有工具：按售前sessionId查找Netty通道
     */
    private Channel findPreSaleChannelBySessionId(String sessionId) {
        Map<String, Channel> channelMap = PreSaleNettyWebSocketServerHandler.PRE_SALE_RECEIVER_CHANNEL_MAP;
        if (CollectionUtils.isEmpty(channelMap)) {
            return null;
        }
        for (Channel channel : channelMap.values()) {
            String bindSessionId = channel.attr(NettyConstant.PRE_SALE_SESSION_ID_KEY).get();
            if (sessionId.equals(bindSessionId)) {
                return channel;
            }
        }
        return null;
    }

    /**
     * 生成售前会话ID（对齐UUIDUtil风格，增加异常兜底）
     */
    @Override
    public String generatePreSaleSessionId() {
        try {
            // 复用UUIDUtil的无横线UUID生成逻辑，保持风格统一
            return PRE_SESSION_PREFIX + UUIDUtil.getUUID();
        } catch (Exception e) {
            // 兜底：直接使用Hutool UUID，避免工具类异常导致生成失败
            logger.warn("使用UUIDUtil生成售前会话ID失败，降级使用Hutool UUID，异常：{}", e.getMessage());
            return PRE_SESSION_PREFIX + UUID.randomUUID().toString().replace("-", "");
        }
    }

    @Override
    public String generatePreSaleMsgId(String preSaleSessionId) {
        // 1. 时间戳（毫秒级）
        long timestamp = System.currentTimeMillis();
        // 2. 会话后缀（安全截取，避免索引越界）
        String sessionSuffix = getSafeSessionSuffix(preSaleSessionId);
        // 3. 拼接最终格式（确保PRE_MSG_前缀不缺失）
        return PRE_MSG_PREFIX + timestamp + "_" + sessionSuffix;
    }

    /**
     * 安全获取会话ID后缀（8位），兼容异常场景
     */
    private String getSafeSessionSuffix(String preSaleSessionId) {
        // 校验会话ID是否为空/格式错误
        if (!StringUtils.hasText(preSaleSessionId) || !preSaleSessionId.startsWith(PRE_SESSION_PREFIX)) {
            logger.warn("售前会话ID格式异常：{}，使用随机后缀兜底", preSaleSessionId);
            // 生成8位随机字符串兜底
            return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        }

        // 截取会话ID前缀后的部分
        String sessionWithoutPrefix = preSaleSessionId.substring(PRE_SESSION_PREFIX.length());
        // 确保截取长度不超过字符串长度
        int suffixStartIndex = Math.max(0, sessionWithoutPrefix.length() - SESSION_SUFFIX_LENGTH);
        String suffix = sessionWithoutPrefix.substring(suffixStartIndex);
        // 兜底：如果截取后不足8位，补0
        if (suffix.length() < SESSION_SUFFIX_LENGTH) {
            suffix = String.format("%-" + SESSION_SUFFIX_LENGTH + "s", suffix).replace(' ', '0');
        }
        return suffix;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> savePreSaleChatMessage(PreSaleChatMessageVO vo, String receiverId) {
        try {
            // 1. 权限校验
            if (!receiverIdService.validateReceiverId(receiverId)) {
                return Result.unauthorized("ReceiverId无效或已过期");
            }
            // 2. 参数校验
            if (vo == null || !StringUtils.hasText(vo.getSenderId()) || !StringUtils.hasText(vo.getPreSaleSessionId())) {
                return Result.paramError("发送者ID、会话ID不能为空");
            }
            if (!StringUtils.hasText(vo.getContent())) {
                return Result.paramError("消息内容不能为空");
            }

            // 3. 处理ReceiverId前缀（和售后对齐）
            String realReceiverId = receiverId.startsWith("R_FIXED_0000_")
                    ? receiverId.substring("R_FIXED_0000_".length())
                    : receiverId;

            // 4. 生成规范msgId（覆盖传入的无效msgId）
            String standardMsgId = generatePreSaleMsgId(vo.getPreSaleSessionId());
            vo.setMsgId(standardMsgId);

            // 5. 补全默认值
            if (vo.getSenderType() == null) {
                vo.setSenderType(PreSaleChatMessageVO.SENDER_TYPE_USER);
            }
            if (vo.getMsgType() == null) {
                vo.setMsgType(PreSaleChatMessageVO.MSG_TYPE_TEXT);
            }
            if (vo.getIsRead() == null) {
                vo.setIsRead(PreSaleChatMessageVO.IS_READ_NO);
            }
            if (vo.getSendTime() == null) {
                vo.setSendTime(new Date());
            }

            // 6. VO转PO并保存
            PreSaleChatMessagePO po = new PreSaleChatMessagePO();
            BeanUtils.copyProperties(vo, po);
            int affectedRows = preSaleChatMessageMapper.insertPreSaleChatMessage(po);
            if (affectedRows <= 0) {
                return Result.error("保存售前消息失败");
            }

            // 7. 入库成功后更新Redis未读消息数（原子操作，高并发安全）
            if (StringUtils.hasText(vo.getReceiverId())) {
                preSaleRedisUnreadMsgCacheService.incrUnreadMsgCount(vo.getReceiverId());
            }

            logger.info("【售前消息保存成功】msgId：{}，会话ID：{}，接收者：{}",
                    standardMsgId, vo.getPreSaleSessionId(), vo.getReceiverId());
            return Result.success();

        } catch (Exception e) {
            // 新增：调用工具方法判断是否为唯一索引冲突
            if (isDuplicateKeyException(e, "uk_pre_msg_id")) {
                logger.info("【售前消息幂等校验】消息已存在，msgId：{}", vo.getMsgId());
                return Result.success();
            }
            logger.error("保存售前消息异常", e);
            return Result.error("保存售前消息异常：" + e.getMessage());
        }
    }

    /**
     * 工具方法：判断异常是否为指定唯一索引的重复键冲突
     * @param e 原始异常
     * @param indexName 唯一索引名（如uk_pre_msg_id）
     * @return 是否为目标索引的重复冲突
     */
    private boolean isDuplicateKeyException(Exception e, String indexName) {
        // 1. 获取根异常（穿透包装异常，找到最底层的SQL异常）
        Throwable rootCause = e;
        while (rootCause.getCause() != null && rootCause != rootCause.getCause()) {
            rootCause = rootCause.getCause();
        }

        // 2. 判断是否为MySQL唯一索引冲突异常（SQLIntegrityConstraintViolationException）
        if (rootCause instanceof java.sql.SQLIntegrityConstraintViolationException) {
            String errorMsg = rootCause.getMessage().toLowerCase();
            // 3. 兼容中英文异常消息的核心特征匹配
            boolean isDuplicateKey = errorMsg.contains("duplicate entry") // 英文特征
                    || errorMsg.contains("重复条目") // 中文特征
                    || errorMsg.contains("唯一索引冲突"); // 框架包装后的特征
            // 4. 匹配目标索引名（兼容带表名的情况，如pre_sale_chat_message.uk_pre_msg_id）
            boolean isTargetIndex = errorMsg.contains(indexName.toLowerCase());
            return isDuplicateKey && isTargetIndex;
        }
        return false;
    }


    @Override
    public Result<List<PreSaleChatMessagePO>> listUnreadBySessionAndReceiver(String sessionId, String receiverId) {
        try {
            // 关键：如果authReceiverId带前缀，这里需要去掉前缀（R_FIXED_0000_）
            String realReceiverId = receiverId.startsWith("R_FIXED_0000_")
                    ? receiverId.substring("R_FIXED_0000_".length())
                    : receiverId;

            List<PreSaleChatMessagePO> unreadList = preSaleChatMessageMapper.listUnreadBySessionIdAndReceiverId(sessionId, realReceiverId);
            return Result.success(unreadList);
        } catch (Exception e) {
            logger.error("查询售前未读消息失败", e);
            return Result.error("查询未读消息失败");
        }
    }

    /**
     * 【新增】分页查询售前会话消息（和售后对齐）
     */
    @Override
    public Result<List<PreSaleChatMessageVO>> listByPreSaleSessionIdWithPage(
            String preSaleSessionId, String receiverId, Integer pageNum, Integer pageSize) {
        try {
            // 1. 权限校验
            if (!receiverIdService.validateReceiverId(receiverId)) {
                return Result.unauthorized("ReceiverId无效或已过期");
            }

            // 2. 分页参数兜底
            pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
            pageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);

            // 3. 处理ReceiverId前缀
            String realReceiverId = receiverId.startsWith("R_FIXED_0000_")
                    ? receiverId.substring("R_FIXED_0000_".length())
                    : receiverId;

            // 4. 开启分页
            PageHelper.startPage(pageNum, pageSize);
            List<PreSaleChatMessagePO> poList = preSaleChatMessageMapper.listByPreSaleSessionId(preSaleSessionId);
            PageInfo<PreSaleChatMessagePO> pageInfo = new PageInfo<>(poList);

            // 5. PO转VO
            List<PreSaleChatMessageVO> voList = new ArrayList<>();
            if (!CollectionUtils.isEmpty(poList)) {
                poList.forEach(po -> {
                    PreSaleChatMessageVO vo = new PreSaleChatMessageVO();
                    BeanUtils.copyProperties(po, vo);
                    voList.add(vo);
                });
            }

            logger.info("【售前分页查询】会话ID：{}，接收者：{}，当前页：{}，总条数：{}",
                    preSaleSessionId, receiverId, pageInfo.getPageNum(), pageInfo.getTotal());

            // 6. 刷新ReceiverId过期时间
            receiverIdService.refreshReceiverIdExpire(receiverId);
            return Result.success(voList);

        } catch (Exception e) {
            logger.error("分页查询售前会话消息异常", e);
            return Result.error("分页查询售前会话消息异常：" + e.getMessage());
        }
    }

    @Override
    public Result<List<PreSaleChatMessageVO>> listByUserId(String userId, String receiverId) {
        try {
            // 1. 权限校验
            if (!receiverIdService.validateReceiverId(receiverId)) {
                return Result.unauthorized("ReceiverId无效或已过期");
            }

            // 2. 处理ReceiverId前缀
            String realReceiverId = receiverId.startsWith("R_FIXED_0000_")
                    ? receiverId.substring("R_FIXED_0000_".length())
                    : receiverId;

            // 3. 查询消息列表
            List<PreSaleChatMessagePO> poList = preSaleChatMessageMapper.listByUserId(userId);
            List<PreSaleChatMessageVO> voList = new ArrayList<>();
            if (!CollectionUtils.isEmpty(poList)) {
                poList.forEach(po -> {
                    PreSaleChatMessageVO vo = new PreSaleChatMessageVO();
                    BeanUtils.copyProperties(po, vo);
                    voList.add(vo);
                });
            }

            // 4. 刷新ReceiverId过期时间
            receiverIdService.refreshReceiverIdExpire(receiverId);
            return Result.success(voList);

        } catch (Exception e) {
            logger.error("查询用户售前消息异常", e);
            return Result.error("查询用户售前消息异常：" + e.getMessage());
        }
    }

    /**
     * 【新增】批量标记售前会话未读消息为已读（和售后对齐）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> batchMarkMsgAsReadBySessionId(String sessionId, String receiverId) {
        try {
            // 1. 权限校验
            if (!receiverIdService.validateReceiverId(receiverId)) {
                return Result.unauthorized("ReceiverId无效或已过期");
            }
            if (!StringUtils.hasText(sessionId)) {
                return Result.paramError("会话ID不能为空");
            }

            // 2. 处理ReceiverId前缀
            String realReceiverId = receiverId.startsWith("R_FIXED_0000_")
                    ? receiverId.substring("R_FIXED_0000_".length())
                    : receiverId;

            // 3. 查询未读消息数
            int unreadCount = preSaleChatMessageMapper.countUnreadMsgBySessionIdAndReceiverId(sessionId, realReceiverId);
            if (unreadCount <= 0) {
                return Result.success(true);
            }

            // 4. 批量标记已读
            int updateResult = preSaleChatMessageMapper.batchUpdateMsgReadStatusBySessionId(sessionId, realReceiverId);
            if (updateResult <= 0) {
                logger.info("【售前批量标记已读】无未读消息需要更新，会话ID：{}", sessionId);
                return Result.success(true);
            }

            // 5. 更新Redis未读消息数（递减）
            preSaleRedisUnreadMsgCacheService.decrUnreadMsgCount(realReceiverId, unreadCount);

            logger.info("【售前批量标记已读成功】会话ID：{}，接收者：{}，标记{}条消息为已读",
                    sessionId, receiverId, updateResult);

            // 6. 刷新ReceiverId过期时间
            receiverIdService.refreshReceiverIdExpire(receiverId);
            return Result.success(true);

        } catch (Exception e) {
            logger.error("批量标记售前消息已读异常", e);
            return Result.error("批量标记售前消息已读异常：" + e.getMessage());
        }
    }

    /**
     * 【新增】查询接收方未读消息总数（优先Redis，兜底DB）
     */
    @Override
    public Result<Long> getUnreadMsgTotalCount(String receiverId) {
        try {
            // 1. 权限校验
            if (!receiverIdService.validateReceiverId(receiverId)) {
                return Result.unauthorized("ReceiverId无效或已过期");
            }

            // 2. 处理ReceiverId前缀
            String realReceiverId = receiverId.startsWith("R_FIXED_0000_")
                    ? receiverId.substring("R_FIXED_0000_".length())
                    : receiverId;

            // 3. 优先查Redis，兜底查DB
            long unreadTotal = preSaleRedisUnreadMsgCacheService.getUnreadMsgCount(realReceiverId, () -> preSaleChatMessageMapper.countTotalUnreadMsgByReceiverId(realReceiverId));

            logger.info("【售前未读总数查询】接收者：{}，未读总数：{}", receiverId, unreadTotal);
            return Result.success(unreadTotal);

        } catch (Exception e) {
            logger.error("查询售前未读消息总数异常", e);
            return Result.error("查询售前未读消息总数异常：" + e.getMessage());
        }
    }

    /**
     * 【新增】按会话ID删除售前会话所有消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> deleteMessageBySessionId(String sessionId, String receiverId) {
        try {
            // 1. 权限校验
            if (!receiverIdService.validateReceiverId(receiverId)) {
                return Result.unauthorized("ReceiverId无效或已过期，无删除权限");
            }
            if (!StringUtils.hasText(sessionId)) {
                return Result.paramError("会话ID不能为空");
            }

            // 2. 校验会话是否存在
            List<PreSaleChatMessagePO> msgList = preSaleChatMessageMapper.listByPreSaleSessionId(sessionId);
            if (CollectionUtils.isEmpty(msgList)) {
                return Result.error("该售前会话无消息记录，无需删除");
            }

            // 3. 执行删除
            int deleteResult = preSaleChatMessageMapper.deleteBySessionId(sessionId);
            if (deleteResult <= 0) {
                return Result.error("删除售前会话消息失败");
            }

            // 4. 刷新ReceiverId过期时间
            receiverIdService.refreshReceiverIdExpire(receiverId);

            logger.info("【售前会话消息删除成功】会话ID：{}，删除{}条记录", sessionId, deleteResult);
            return Result.success(true);

        } catch (Exception e) {
            logger.error("删除售前会话消息异常", e);
            return Result.error("删除售前会话消息异常：" + e.getMessage());
        }
    }

    @Override
    public Result<Void> cleanExpiredPreSaleChatMessage() {
        try {
            Date expireTime = DateUtil.offsetDay(new Date(), -7);
            int affectedRows = preSaleChatMessageMapper.deleteExpiredPreSaleChatMessage(expireTime);
            logger.info("清理过期售前消息成功，共清理{}条记录", affectedRows);
            return Result.success();
        } catch (Exception e) {
            logger.error("清理过期售前消息异常", e);
            return Result.error("清理过期售前消息异常：" + e.getMessage());
        }
    }
}