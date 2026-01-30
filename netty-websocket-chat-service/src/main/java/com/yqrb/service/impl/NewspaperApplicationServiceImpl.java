package com.yqrb.service.impl;

import com.yqrb.mapper.NewspaperApplicationMapperCustom;
import com.yqrb.mapper.SessionMappingMapperCustom;
import com.yqrb.netty.NettyWebSocketUtil;
import com.yqrb.pojo.vo.*;
import com.yqrb.service.*;
import com.yqrb.util.DateUtil;
import com.yqrb.util.UUIDUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.springframework.beans.factory.annotation.Autowired;
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

    // 新增：注入 Netty WebSocket 工具类
    @Resource
    private NettyWebSocketUtil nettyWebSocketUtil;

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

    // 其他原有注入
    @Autowired
    private OfflineMsgService offlineMsgService;

    // 新增：系统自动分配在线客服的私有方法
// 优化：返回Result<String>，统一响应格式，避免抛未捕获异常
// 优化：新增receiverId参数，传递给客服查询方法
    private Result<String> autoAssignOnlineCustomer(String receiverId) {
        // 1. 调用客服服务，查询所有在线客服（传入有效的receiverId）
        Result<List<CustomerServiceVO>> onlineCsResult = customerServiceService.getOnlineCustomerList(receiverId);

        // 2. 完整校验查询结果（按优先级：先判空→再判是否成功→最后判数据是否为空）
        if (onlineCsResult == null) {
            return Result.error("查询在线客服失败：服务返回空结果");
        }
        if (!onlineCsResult.isSuccess()) {
            return Result.error("查询在线客服失败：" + onlineCsResult.getMsg());
        }
        List<CustomerServiceVO> onlineCsList = onlineCsResult.getData();
        if (onlineCsList == null || onlineCsList.isEmpty()) {
            return Result.error("当前无在线客服，无法提交申请");
        }

        // 3. 随机选一个在线客服
        int randomIndex = new Random().nextInt(onlineCsList.size());
        String serviceStaffId = onlineCsList.get(randomIndex).getServiceStaffId();
        return Result.success(serviceStaffId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<NewspaperApplicationVO> submitApplication(NewspaperApplicationVO application, String receiverId) {
        // **************** 原有业务逻辑（入库等） ****************
        // 1. 补全申请信息（appId、createTime等）
        // 2. 插入登报申请数据
        // 3. 插入会话映射数据
        // ... 省略你的原有代码 ...

        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 2. 自动分配客服（适配Result返回值）
        // 2. 自动分配客服（传递有效的receiverId）
        if (!StringUtils.hasText(application.getServiceStaffId())) {
            // 关键修改：传入有效的receiverId，而非让getOnlineCustomerList接收null
            Result<String> csResult = autoAssignOnlineCustomer(receiverId);
            if (!csResult.isSuccess()) {
                return Result.error(csResult.getMsg());
            }
            application.setServiceStaffId(csResult.getData());
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

        // 7. 🔥 核心新增：推送新申请提醒给对应客服 🔥
        try {
            pushNewApplicationToCs(application, sessionId, currentDate);
        } catch (Exception e) {
            // 推送失败不影响主流程，仅打错误日志
            System.err.printf("【新申请推送失败】appId：%s，客服ID：%s，原因：%s%n",
                    appId, application.getServiceStaffId(), e.getMessage());
        }

        // 8. 刷新ReceiverId过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);

        // 9. 返回申请详情
        NewspaperApplicationVO resultApp = newspaperApplicationMapperCustom.selectByAppId(appId);
        return Result.success(resultApp);
    }

    /**
     * 新增：向客服推送「新申请提醒」WebSocket消息（补充离线消息兜底逻辑）
     * @param application 登报申请信息
     * @param sessionId 会话ID
     * @param submitTime 提交时间
     */
    private void pushNewApplicationToCs(NewspaperApplicationVO application, String sessionId, Date submitTime) {
        // 1. 获取推送目标（客服的 receiverId = serviceStaffId）
        String csReceiverId = application.getServiceStaffId();
        String appId = application.getAppId();
        if (!StringUtils.hasText(csReceiverId)) {
            System.err.println("【新申请推送】客服ID为空，跳过推送与离线存储");
            return;
        }

        // 2. 构建统一消息内容（复用，同时用于在线推送和离线存储）
        String msgContent = String.format(
                "【新登报申请提醒】%n" +
                        "申请ID：%s%n" +
                        "申请人：%s%n" +
                        "联系电话：%s%n" +
                        "申请类型：%s%n" +
                        "提交时间：%s",
                appId,
                application.getUserName(),
                application.getUserPhone(),
                application.getCertType(),
                DateUtil.formatDate(submitTime, "yyyy-MM-dd HH:mm:ss")
        );

        // 3. 构建离线消息VO（用于客服离线/推送失败时存储）
        OfflineMsgVO offlineMsgVO = this.buildOfflineMsgVO(application, msgContent, submitTime);

        // 4. 校验客服是否在线（有活跃的 WebSocket 通道）
        if (nettyWebSocketUtil.isReceiverOnline(csReceiverId)) {
            // 情况1：客服在线，尝试实时推送WebSocket消息
            try {
                // 封装 WebSocket 消息对象
                WebSocketMsgVO newAppMsg = new WebSocketMsgVO();
                newAppMsg.setReceiverId(csReceiverId); // 接收者：客服
                newAppMsg.setUserId("SYSTEM"); // 发送者：系统
                newAppMsg.setSenderType(WebSocketMsgVO.SENDER_TYPE_SYSTEM); // 发送者类型：系统
                newAppMsg.setMsgContent(msgContent); // 提醒内容
                newAppMsg.setMsgType(WebSocketMsgVO.MSG_TYPE_NEW_APPLICATION); // 专属消息类型
                newAppMsg.setSessionId(sessionId); // 绑定会话ID
                newAppMsg.setSendTime(submitTime); // 发送时间 = 提交时间

                // 获取客服通道，推送消息
                Channel csChannel = nettyWebSocketUtil.getChannelByReceiverId(csReceiverId);
                if (csChannel != null) {
                    String jsonMsg = com.alibaba.fastjson.JSON.toJSONString(newAppMsg);
                    csChannel.writeAndFlush(new TextWebSocketFrame(jsonMsg));
                    System.out.printf("【新申请推送成功】客服%s已收到申请%s的提醒%n", csReceiverId, appId);
                }
            } catch (Exception e) {
                // 推送失败：降级存储为离线消息（兜底，避免消息丢失）
                System.err.printf("【新申请推送异常】客服%s，申请%s，原因：%s，已触发离线消息兜底%n",
                        csReceiverId, appId, e.getMessage());
                this.saveOfflineMsgFallback(offlineMsgVO);
            }
        } else {
            // 情况2：客服离线，直接存储为离线消息（后续上线补偿）
            System.out.printf("【新申请推送】客服%s未在线，已存储为离线消息%n", csReceiverId);
            this.saveOfflineMsgFallback(offlineMsgVO);
        }
    }

    /**
     * 辅助方法：构建离线消息VO（封装重复逻辑，提高可维护性）
     * @param application 登报申请信息
     * @param msgContent 消息内容
     * @param submitTime 提交时间
     * @return 离线消息VO
     */
    private OfflineMsgVO buildOfflineMsgVO(NewspaperApplicationVO application, String msgContent, Date submitTime) {
        OfflineMsgVO offlineMsgVO = new OfflineMsgVO();
        // 补全离线消息核心字段（与数据库表对应）
        offlineMsgVO.setServiceStaffId(application.getServiceStaffId()); // 目标客服ID
        offlineMsgVO.setMsgType(WebSocketMsgVO.MSG_TYPE_NEW_APPLICATION); // 消息类型（与WebSocket一致）
        offlineMsgVO.setAppId(application.getAppId()); // 申请ID（关联登报申请）
        offlineMsgVO.setMsgContent(msgContent); // 消息内容（与在线推送一致）
        offlineMsgVO.setIsPushed(0); // 0=未推送（默认值，后续上线补偿后标记为1）
        offlineMsgVO.setCreateTime(submitTime); // 创建时间=申请提交时间
        offlineMsgVO.setUpdateTime(submitTime); // 更新时间=申请提交时间
        return offlineMsgVO;
    }

    /**
     * 降级存储离线消息（客服离线/推送失败时调用，兜底保障消息不丢失）
     */
    private void saveOfflineMsgFallback(OfflineMsgVO offlineMsgVO) {
        try {
            // 调用已注入的OfflineMsgService，存储离线消息
            boolean saveResult = offlineMsgService.saveOfflineMsg(offlineMsgVO);
            if (!saveResult) {
                System.err.printf("【离线消息存储失败】客服%s，申请%s%n",
                        offlineMsgVO.getServiceStaffId(), offlineMsgVO.getAppId());
            }
        } catch (Exception e) {
            System.err.printf("【离线消息存储异常】客服%s，申请%s，原因：%s%n",
                    offlineMsgVO.getServiceStaffId(), offlineMsgVO.getAppId(), e.getMessage());
        }
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
                payMsg.setSenderType("CS"); // 发送者类型：USER/CS/SYSTEM
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
