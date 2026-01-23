package com.yqrb.service.impl;

import com.yqrb.mapper.NewspaperApplicationMapperCustom;
import com.yqrb.mapper.SessionMappingMapperCustom;
import com.yqrb.pojo.vo.NewspaperApplicationVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.pojo.vo.SessionMappingVO;
import com.yqrb.service.NewspaperApplicationService;
import com.yqrb.service.ReceiverIdService;
import com.yqrb.util.DateUtil;
import com.yqrb.util.UUIDUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
public class NewspaperApplicationServiceImpl implements NewspaperApplicationService {

    @Resource
    private NewspaperApplicationMapperCustom newspaperApplicationMapperCustom;

    @Resource
    private SessionMappingMapperCustom sessionMappingMapperCustom;

    @Resource
    private ReceiverIdService receiverIdService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<NewspaperApplicationVO> submitApplication(NewspaperApplicationVO application, String receiverId) {
        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
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
        if (application.getPayAmount() == null) {
            application.setPayAmount(java.math.BigDecimal.ZERO);
        }

        // 3. 校验客服是否存在（简单校验）
        // 此处可扩展：关联CustomerServiceMapper查询客服有效性

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
    public Result<Boolean> auditApplication(String appId, String status, String auditRemark, String receiverId) {
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 校验申请是否存在
        NewspaperApplicationVO application = newspaperApplicationMapperCustom.selectByAppId(appId);
        if (application == null) {
            return Result.error("登报申请不存在");
        }

        // 校验状态有效性
        if (!status.equals(NewspaperApplicationVO.STATUS_AUDITED) &&
                !status.equals(NewspaperApplicationVO.STATUS_REJECTED) &&
                !status.equals(NewspaperApplicationVO.STATUS_PAID)) {
            return Result.paramError("无效的申请状态");
        }

        // 补全审核信息
        Date currentDate = DateUtil.getCurrentDate();
        application.setStatus(status);
        application.setAuditRemark(auditRemark);
        application.setAuditTime(currentDate);
        application.setUpdateTime(currentDate);

        // 若为支付状态，补全支付时间
        if (status.equals(NewspaperApplicationVO.STATUS_PAID)) {
            application.setPayTime(currentDate);
        }

        // 更新申请状态
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