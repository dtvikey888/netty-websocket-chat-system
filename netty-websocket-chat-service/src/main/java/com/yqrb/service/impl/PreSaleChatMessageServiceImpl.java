package com.yqrb.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import com.yqrb.mapper.PreSaleChatMessageMapper;
import com.yqrb.pojo.po.PreSaleChatMessagePO;
import com.yqrb.pojo.vo.PreSaleChatMessageVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.PreSaleChatMessageService;
import com.yqrb.service.ReceiverIdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 售前咨询聊天记录Service实现类
 */
@Service
public class PreSaleChatMessageServiceImpl implements PreSaleChatMessageService {
    private static final Logger logger = LoggerFactory.getLogger(PreSaleChatMessageServiceImpl.class);

    @Resource
    private PreSaleChatMessageMapper preSaleChatMessageMapper;

    @Resource
    private ReceiverIdService receiverIdService;

    @Override
    public String generatePreSaleSessionId() {
        return "PRE_SESSION_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public String generatePreSaleMsgId(String preSaleSessionId) {
        long timestamp = System.currentTimeMillis();
        String sessionSuffix = preSaleSessionId.substring(preSaleSessionId.length() - 8);
        return "PRE_MSG_" + timestamp + "_" + sessionSuffix;
    }

    @Override
    public Result<Void> savePreSaleChatMessage(PreSaleChatMessageVO vo, String receiverId) {
        try {
            if (!receiverIdService.validateReceiverId(receiverId)) {
                return Result.unauthorized("ReceiverId无效或已过期");
            }
            if (vo == null || vo.getSenderId() == null || vo.getPreSaleSessionId() == null) {
                return Result.error("必要参数不能为空");
            }
            if (vo.getMsgId() == null) {
                vo.setMsgId(generatePreSaleMsgId(vo.getPreSaleSessionId()));
            }
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

            PreSaleChatMessagePO po = new PreSaleChatMessagePO();
            BeanUtils.copyProperties(vo, po);
            int affectedRows = preSaleChatMessageMapper.insertPreSaleChatMessage(po);
            return affectedRows > 0 ? Result.success() : Result.error("保存售前消息失败");
        } catch (Exception e) {
            logger.error("保存售前消息异常", e);
            return Result.error("保存售前消息异常：" + e.getMessage());
        }
    }

    @Override
    public Result<List<PreSaleChatMessageVO>> listByPreSaleSessionId(String preSaleSessionId, String receiverId) {
        try {
            if (!receiverIdService.validateReceiverId(receiverId)) {
                return Result.unauthorized("ReceiverId无效或已过期");
            }
            List<PreSaleChatMessagePO> poList = preSaleChatMessageMapper.listByPreSaleSessionId(preSaleSessionId);
            List<PreSaleChatMessageVO> voList = new ArrayList<>();
            if (!CollectionUtils.isEmpty(poList)) {
                poList.forEach(po -> {
                    PreSaleChatMessageVO vo = new PreSaleChatMessageVO();
                    BeanUtils.copyProperties(po, vo);
                    voList.add(vo);
                });
            }
            return Result.success(voList);
        } catch (Exception e) {
            logger.error("查询售前会话消息异常", e);
            return Result.error("查询售前会话消息异常：" + e.getMessage());
        }
    }

    @Override
    public Result<List<PreSaleChatMessageVO>> listByUserId(String userId, String receiverId) {
        try {
            if (!receiverIdService.validateReceiverId(receiverId)) {
                return Result.unauthorized("ReceiverId无效或已过期");
            }
            List<PreSaleChatMessagePO> poList = preSaleChatMessageMapper.listByUserId(userId);
            List<PreSaleChatMessageVO> voList = new ArrayList<>();
            if (!CollectionUtils.isEmpty(poList)) {
                poList.forEach(po -> {
                    PreSaleChatMessageVO vo = new PreSaleChatMessageVO();
                    BeanUtils.copyProperties(po, vo);
                    voList.add(vo);
                });
            }
            return Result.success(voList);
        } catch (Exception e) {
            logger.error("查询用户售前消息异常", e);
            return Result.error("查询用户售前消息异常：" + e.getMessage());
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