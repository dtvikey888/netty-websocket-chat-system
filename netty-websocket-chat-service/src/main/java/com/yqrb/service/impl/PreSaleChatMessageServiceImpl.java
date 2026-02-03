package com.yqrb.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import com.yqrb.mapper.PreSaleChatMessageMapper;
import com.yqrb.pojo.po.PreSaleChatMessagePO;
import com.yqrb.pojo.vo.PreSaleChatMessageVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.PreSaleChatMessageService;
import com.yqrb.service.ReceiverIdService;
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

    @Resource
    private PreSaleChatMessageMapper preSaleChatMessageMapper;

    @Resource
    private ReceiverIdService receiverIdService;

    /**
     * 生成售前会话ID
     */
    @Override
    public String generatePreSaleSessionId() {
        return "PRE_SESSION_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成售前消息ID
     */
    @Override
    public String generatePreSaleMsgId(String preSaleSessionId) {
        // 格式：PRE_MSG_+时间戳+会话ID后8位，保证唯一性且便于追溯
        long timestamp = System.currentTimeMillis();
        String sessionSuffix = preSaleSessionId.substring(preSaleSessionId.length() - 8);
        return "PRE_MSG_" + timestamp + "_" + sessionSuffix;
    }

    /**
     * 保存售前消息
     */
    @Override
    public Result<Void> savePreSaleChatMessage(PreSaleChatMessageVO vo, String receiverId) {
        try {
            // 1. 校验ReceiverId有效性
            if (!receiverIdService.validateReceiverId(receiverId)) {
                return Result.unauthorized("ReceiverId无效或已过期");
            }

            // 1. 校验必要参数
            if (vo == null || vo.getSenderId() == null || vo.getPreSaleSessionId() == null) {
                return Result.error("必要参数不能为空");
            }

            // 2. 补全默认值
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

            // 3. VO 转 PO
            PreSaleChatMessagePO po = new PreSaleChatMessagePO();
            BeanUtils.copyProperties(vo, po);

            // 4. 插入数据库
            int affectedRows = preSaleChatMessageMapper.insertPreSaleChatMessage(po);
            return affectedRows > 0 ? Result.success() : Result.error("保存售前消息失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("保存售前消息异常：" + e.getMessage());
        }
    }

    /**
     * 按会话ID查询消息
     */
    @Override
    public Result<List<PreSaleChatMessageVO>> listByPreSaleSessionId(String preSaleSessionId, String receiverId) {
        try {
            // 1. 校验ReceiverId有效性
            if (!receiverIdService.validateReceiverId(receiverId)) {
                return Result.unauthorized("ReceiverId无效或已过期");
            }
            // 1. 查询PO列表
            List<PreSaleChatMessagePO> poList = preSaleChatMessageMapper.listByPreSaleSessionId(preSaleSessionId);
            // 2. PO 转 VO 列表
            List<PreSaleChatMessageVO> voList = new ArrayList<>();
            if (!CollectionUtils.isEmpty(poList)) {
                for (PreSaleChatMessagePO po : poList) {
                    PreSaleChatMessageVO vo = new PreSaleChatMessageVO();
                    BeanUtils.copyProperties(po, vo);
                    voList.add(vo);
                }
            }
            return Result.success(voList);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询售前会话消息异常：" + e.getMessage());
        }
    }

    /**
     * 按用户ID查询消息（供售后追溯）
     */
    @Override
    public Result<List<PreSaleChatMessageVO>> listByUserId(String userId, String receiverId) {
        try {
            // 1. 校验ReceiverId有效性
            if (!receiverIdService.validateReceiverId(receiverId)) {
                return Result.unauthorized("ReceiverId无效或已过期");
            }
            // 1. 查询PO列表
            List<PreSaleChatMessagePO> poList = preSaleChatMessageMapper.listByUserId(userId);
            // 2. PO 转 VO 列表
            List<PreSaleChatMessageVO> voList = new ArrayList<>();
            if (!CollectionUtils.isEmpty(poList)) {
                for (PreSaleChatMessagePO po : poList) {
                    PreSaleChatMessageVO vo = new PreSaleChatMessageVO();
                    BeanUtils.copyProperties(po, vo);
                    voList.add(vo);
                }
            }
            return Result.success(voList);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询用户售前消息异常：" + e.getMessage());
        }
    }

    /**
     * 清理过期消息（7天前）
     */
    @Override
    public Result<Void> cleanExpiredPreSaleChatMessage() {
        try {
            // 计算7天前的时间
            Date expireTime = DateUtil.offsetDay(new Date(), -7);
            // 执行删除
            int affectedRows = preSaleChatMessageMapper.deleteExpiredPreSaleChatMessage(expireTime);
            // 仅打印日志，返回默认成功结果（不修改Result类）
            System.out.println("清理过期售前消息成功，共清理" + affectedRows + "条记录");
            return Result.success(); // 调用原有无参success方法
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("清理过期售前消息异常：" + e.getMessage());
        }
    }
}