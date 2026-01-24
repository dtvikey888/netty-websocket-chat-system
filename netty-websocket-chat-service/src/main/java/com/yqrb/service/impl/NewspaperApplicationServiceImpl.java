package com.yqrb.service.impl;

import com.yqrb.mapper.NewspaperApplicationMapperCustom;
import com.yqrb.mapper.SessionMappingMapperCustom;
import com.yqrb.pojo.vo.*;
import com.yqrb.service.ChatMessageService;
import com.yqrb.service.CustomerServiceService;
import com.yqrb.service.NewspaperApplicationService;
import com.yqrb.service.ReceiverIdService;
import com.yqrb.util.DateUtil;
import com.yqrb.util.UUIDUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Service
public class NewspaperApplicationServiceImpl implements NewspaperApplicationService {

    @Resource
    private NewspaperApplicationMapperCustom newspaperApplicationMapperCustom;

    @Resource
    private SessionMappingMapperCustom sessionMappingMapperCustom;

    @Resource
    private CustomerServiceService customerServiceService;

    // 注入聊天消息服务（原有，若未注入则新增）
    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private ReceiverIdService receiverIdService;

    // 新增：系统自动分配在线客服的私有方法
    private String autoAssignOnlineCustomer() {
        // 调用客服服务，查询所有在线客服
        Result<List<CustomerServiceVO>> onlineCsResult = customerServiceService.getOnlineCustomerList(null);
        if (onlineCsResult.getData() == null || onlineCsResult.getData().isEmpty()) {
            throw new RuntimeException("当前无在线客服，无法提交申请");
        }
        // 简单实现：随机选一个在线客服（可扩展为按接待量/负载均衡）
        List<CustomerServiceVO> onlineCsList = onlineCsResult.getData();
        int randomIndex = new Random().nextInt(onlineCsList.size());
        return onlineCsList.get(randomIndex).getServiceStaffId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<NewspaperApplicationVO> submitApplication(NewspaperApplicationVO application, String receiverId) {
        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 在submitApplication中调用：提交时自动分配客服
        if (!StringUtils.hasText(application.getServiceStaffId())) {
            application.setServiceStaffId(autoAssignOnlineCustomer());
        }

        // 2. 补全申请信息
        String appId = UUIDUtil.generateAppId();
        String sessionId = UUIDUtil.generateSessionId();
        Date currentDate = DateUtil.getCurrentDate();

        application.setAppId(appId);
        application.setStatus(NewspaperApplicationVO.STATUS_PENDING);
        application.setSubmitTime(currentDate);
        application.setCreateTime(currentDate);
        application.setUpdateTime(currentDate);
        // 强制置空金额：由审核人手动设置，客户提交时不赋值
        application.setPayAmount(BigDecimal.ZERO);

        // 3. 补充：校验分配的客服是否存在且在线（避免分配到无效客服）
        Result<CustomerServiceVO> csResult = customerServiceService.getCustomerByStaffId(application.getServiceStaffId(), receiverId);
        if (csResult.getData() == null || !CustomerServiceVO.STATUS_ONLINE.equals(csResult.getData().getStatus())) {
            return Result.paramError("分配的客服不存在或未在线，提交申请失败");
        }

        // 4. 插入登报申请
        int insertResult = newspaperApplicationMapperCustom.insertNewspaperApplication(application);
        if (insertResult <= 0) {
            return Result.error("提交登报申请失败");
        }

        // 5. 插入会话映射
        SessionMappingVO sessionMapping = new SessionMappingVO();
        sessionMapping.setSessionId(sessionId);
        sessionMapping.setAppId(appId);
        sessionMapping.setUserId(application.getUserId());
        sessionMapping.setServiceStaffId(application.getServiceStaffId());
        sessionMapping.setCreateTime(currentDate);
        sessionMapping.setUpdateTime(currentDate);
        sessionMappingMapperCustom.insertSessionMapping(sessionMapping);

        // 6. 刷新ReceiverId过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);

        // 7. 返回申请详情
        NewspaperApplicationVO resultApp = newspaperApplicationMapperCustom.selectByAppId(appId);
        return Result.success(resultApp);
    }

    @Override
    public Result<NewspaperApplicationVO> getApplicationByAppId(String appId, String receiverId) {
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        NewspaperApplicationVO application = newspaperApplicationMapperCustom.selectByAppId(appId);
        if (application == null) {
            return Result.error("登报申请不存在");
        }

        receiverIdService.refreshReceiverIdExpire(receiverId);
        return Result.success(application);
    }

    @Override
    public Result<List<NewspaperApplicationVO>> getApplicationListByUserId(String userId, String receiverId) {
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        List<NewspaperApplicationVO> appList = newspaperApplicationMapperCustom.selectByUserId(userId);
        receiverIdService.refreshReceiverIdExpire(receiverId);
        return Result.success(appList);
    }

    @Override
    public Result<List<NewspaperApplicationVO>> getApplicationListByServiceStaffId(String serviceStaffId, String receiverId) {
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        List<NewspaperApplicationVO> appList = newspaperApplicationMapperCustom.selectByServiceStaffId(serviceStaffId);
        receiverIdService.refreshReceiverIdExpire(receiverId);
        return Result.success(appList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // ========== 修改1：新增BigDecimal payAmount参数（审核人手动设置的付款金额） ==========
    public Result<Boolean> auditApplication(String appId, String status, String auditRemark, BigDecimal payAmount, String receiverId) {
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 校验申请是否存在
        NewspaperApplicationVO application = newspaperApplicationMapperCustom.selectByAppId(appId);
        if (application == null) {
            return Result.error("登报申请不存在");
        }

        // 校验状态有效性（常量前置，规避空指针）
        if (!NewspaperApplicationVO.STATUS_AUDITED.equals(status) &&
                !NewspaperApplicationVO.STATUS_REJECTED.equals(status) &&
                !NewspaperApplicationVO.STATUS_PAID.equals(status)) {
            return Result.paramError("无效的申请状态，仅支持AUDITED/REJECTED/PAID");
        }

        // 补全审核信息
        Date currentDate = DateUtil.getCurrentDate();
        application.setStatus(status);
        application.setAuditRemark(auditRemark);
        application.setAuditTime(currentDate);
        application.setUpdateTime(currentDate);

        // ========== 修改2：审核通过时，校验并设置付款金额（持久化到数据库） ==========
        if (NewspaperApplicationVO.STATUS_AUDITED.equals(status)) {
            // 2.1 校验金额有效性：非空且大于0
            if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return Result.paramError("审核通过时，付款金额不能为空且必须大于0");
            }
            // 2.2 将审核人设置的金额写入申请（持久化）
            application.setPayAmount(payAmount);

            // 2.3 发送客服专属聊天消息（含正确的payAmount）
            try {
                // 1. 根据appId查询会话映射（获取专属sessionId，绑定了用户+客服）
                SessionMappingVO sessionMapping = sessionMappingMapperCustom.selectByAppId(appId);
                if (sessionMapping == null) {
                    throw new RuntimeException("申请未绑定会话，无法发送聊天消息");
                }
                String sessionId = sessionMapping.getSessionId();
                String serviceStaffId = sessionMapping.getServiceStaffId(); // 承接该申请的客服（审核人）

                // 2. 构建聊天消息VO（修正字段错误：userId → senderId）
                WebSocketMsgVO payMsg = new WebSocketMsgVO();
                payMsg.setSessionId(sessionId); // 专属会话ID
                payMsg.setUserId(serviceStaffId); // 发送人：审核客服ID（对应VO的userId字段）
                payMsg.setReceiverId(application.getUserId()); // 接收人：客户ID
                payMsg.setMsgType("TEXT"); // 消息类型：文本
                // 2.4 拼接消息内容（此时payAmount已定义，不会报错）
                payMsg.setMsgContent(String.format("您好，您的登报申请（ID：%s）已审核通过，需支付金额：%s元，请及时完成付款~", appId, payAmount));
                payMsg.setSendTime(currentDate);

                // 3. 调用原有聊天消息服务，发送专属聊天消息（客户可在聊天窗口看到）
                chatMessageService.sendMessage(payMsg, receiverId);

                System.out.printf("客服%s已给客户%s发送付款提醒消息，申请ID：%s，金额：%s%n", serviceStaffId, application.getUserId(), appId, payAmount);
            } catch (Exception e) {
                // 消息发送失败不影响审核主流程，仅打日志
                System.err.printf("发送付款聊天消息失败，appId：%s，原因：%s%n", appId, e.getMessage());
            }
        }

        // 若为支付状态，补全支付时间（常量前置，规避空指针）
        if (NewspaperApplicationVO.STATUS_PAID.equals(status)) {
            application.setPayTime(currentDate);
        }

        // 更新申请状态（含审核人设置的付款金额）
        int updateResult = newspaperApplicationMapperCustom.updateStatusByAppId(application);
        if (updateResult <= 0) {
            return Result.error("审核登报申请失败");
        }

        receiverIdService.refreshReceiverIdExpire(receiverId);
        return Result.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> deleteApplication(String appId, String receiverId) {
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 校验申请是否存在
        NewspaperApplicationVO application = newspaperApplicationMapperCustom.selectByAppId(appId);
        if (application == null) {
            return Result.error("登报申请不存在");
        }

        // 删除申请
        int deleteResult = newspaperApplicationMapperCustom.deleteByAppId(appId);
        if (deleteResult <= 0) {
            return Result.error("删除登报申请失败");
        }

        receiverIdService.refreshReceiverIdExpire(receiverId);
        return Result.success(true);
    }
}
