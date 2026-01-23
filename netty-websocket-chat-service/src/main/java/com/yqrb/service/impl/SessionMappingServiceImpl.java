package com.yqrb.service.impl;
import com.yqrb.mapper.NewspaperApplicationMapperCustom;
import com.yqrb.mapper.SessionMappingMapperCustom;
import com.yqrb.pojo.vo.CustomerServiceVO;
import com.yqrb.pojo.vo.NewspaperApplicationVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.pojo.vo.SessionMappingVO;
import com.yqrb.service.CustomerServiceService;
import com.yqrb.service.ReceiverIdService;
import com.yqrb.service.SessionMappingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
public class SessionMappingServiceImpl implements SessionMappingService {

    @Resource
    private SessionMappingMapperCustom sessionMappingMapperCustom;

    @Resource
    private ReceiverIdService receiverIdService;

    // ========== 新增注入：用于客服校验和联动更新登报申请 ==========
    @Resource
    private CustomerServiceService customerServiceService;
    @Resource
    private NewspaperApplicationMapperCustom newspaperApplicationMapperCustom;

    // ========== 原有方法（保留，补充客服校验+联动更新逻辑） ==========
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SessionMappingVO> createSessionMapping(SessionMappingVO sessionMapping, String receiverId) {
        // 1. 校验ReceiverId
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或过期");
        }
        // 2. 校验必填参数
        if (!StringUtils.hasText(sessionMapping.getSessionId()) || !StringUtils.hasText(sessionMapping.getAppId())
                || !StringUtils.hasText(sessionMapping.getUserId())) {
            return Result.paramError("sessionId、appId、userId不能为空");
        }
        // 3. 避免重复创建（新增：调用扩展查询方法）
        SessionMappingVO exist = sessionMappingMapperCustom.selectByUserIdAndAppId(sessionMapping);
        if (exist != null) {
            return Result.error("该用户+申请已存在会话，无需重复创建");
        }

        // ========== 修正：客服有效性校验（使用正确的status字段+枚举常量） ==========
        if (StringUtils.hasText(sessionMapping.getServiceStaffId())) {
            Result<CustomerServiceVO> csResult = customerServiceService.getCustomerByStaffId(sessionMapping.getServiceStaffId(), receiverId);
            if (csResult.getData() == null || !CustomerServiceVO.STATUS_ONLINE.equals(csResult.getData().getStatus())) {
                return Result.paramError("客服不存在或未在线");
            }
        }

        // 4. 补全时间字段
        sessionMapping.setCreateTime(new Date());
        sessionMapping.setUpdateTime(new Date());
        // 5. 插入数据
        int insert = sessionMappingMapperCustom.insertSessionMapping(sessionMapping);
        if (insert <= 0) {
            return Result.error("创建会话映射失败");
        }

        // ========== 新增：联动更新登报申请的service_staff_id ==========
        if (StringUtils.hasText(sessionMapping.getServiceStaffId()) && StringUtils.hasText(sessionMapping.getAppId())) {
            NewspaperApplicationVO application = newspaperApplicationMapperCustom.selectByAppId(sessionMapping.getAppId());
            if (application != null) {
                application.setServiceStaffId(sessionMapping.getServiceStaffId());
                application.setUpdateTime(new Date());
                newspaperApplicationMapperCustom.updateStatusByAppId(application);
            }
        }

        // 6. 刷新ReceiverId过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);
        return Result.success(sessionMapping);
    }

    @Override
    public Result<SessionMappingVO> getBySessionId(String sessionId, String receiverId) {
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或过期");
        }
        SessionMappingVO vo = sessionMappingMapperCustom.selectBySessionId(sessionId);
        receiverIdService.refreshReceiverIdExpire(receiverId);
        return vo == null ? Result.error("会话映射不存在") : Result.success(vo);
    }

    @Override
    public Result<SessionMappingVO> getByAppId(String appId, String receiverId) {
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或过期");
        }
        SessionMappingVO vo = sessionMappingMapperCustom.selectByAppId(appId);
        receiverIdService.refreshReceiverIdExpire(receiverId);
        return vo == null ? Result.error("会话映射不存在") : Result.success(vo);
    }

    @Override
    public Result<List<SessionMappingVO>> getByUserId(String userId, String receiverId) {
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或过期");
        }
        List<SessionMappingVO> list = sessionMappingMapperCustom.selectByUserId(userId);
        receiverIdService.refreshReceiverIdExpire(receiverId);
        return Result.success(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> deleteBySessionId(String sessionId, String receiverId) {
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或过期");
        }
        if (!StringUtils.hasText(sessionId)) {
            return Result.paramError("sessionId不能为空");
        }
        SessionMappingVO exist = sessionMappingMapperCustom.selectBySessionId(sessionId);
        if (exist == null) {
            return Result.error("会话映射不存在");
        }
        int delete = sessionMappingMapperCustom.deleteBySessionId(sessionId);
        receiverIdService.refreshReceiverIdExpire(receiverId);
        return delete > 0 ? Result.success(true) : Result.error("删除失败");
    }

    // ========== 新增缺失方法的实现（补充客服校验+联动更新逻辑） ==========
    @Override
    public Result<List<SessionMappingVO>> getByServiceStaffId(String serviceStaffId, String receiverId) {
        // 1. 鉴权
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或过期");
        }
        // 2. 参数校验
        if (!StringUtils.hasText(serviceStaffId)) {
            return Result.paramError("serviceStaffId不能为空");
        }

        // ========== 修正：客服有效性校验（仅校验存在性，无需在线） ==========
        Result<CustomerServiceVO> csResult = customerServiceService.getCustomerByStaffId(serviceStaffId, receiverId);
        if (csResult.getData() == null) {
            return Result.paramError("客服不存在");
        }

        // 3. 查询数据
        List<SessionMappingVO> list = sessionMappingMapperCustom.selectByServiceStaffId(serviceStaffId);
        // 4. 刷新ReceiverId
        receiverIdService.refreshReceiverIdExpire(receiverId);
        return Result.success(list);
    }

    @Override
    public Result<SessionMappingVO> getByUserIdAndAppId(String userId, String appId, String receiverId) {
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或过期");
        }
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(appId)) {
            return Result.paramError("userId和appId不能为空");
        }
        SessionMappingVO param = new SessionMappingVO();
        param.setUserId(userId);
        param.setAppId(appId);
        SessionMappingVO vo = sessionMappingMapperCustom.selectByUserIdAndAppId(param);
        receiverIdService.refreshReceiverIdExpire(receiverId);
        return vo == null ? Result.error("会话映射不存在") : Result.success(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> updateSessionMapping(SessionMappingVO sessionMapping, String receiverId) {
        // 1. 鉴权
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或过期");
        }
        // 2. 参数校验
        if (!StringUtils.hasText(sessionMapping.getSessionId()) || !StringUtils.hasText(sessionMapping.getServiceStaffId())) {
            return Result.paramError("sessionId和serviceStaffId不能为空");
        }

        // ========== 修正：客服有效性校验（使用正确的status字段+枚举常量） ==========
        Result<CustomerServiceVO> csResult = customerServiceService.getCustomerByStaffId(sessionMapping.getServiceStaffId(), receiverId);
        if (csResult.getData() == null || !CustomerServiceVO.STATUS_ONLINE.equals(csResult.getData().getStatus())) {
            return Result.paramError("客服不存在或未在线");
        }

        // 3. 校验会话是否存在
        SessionMappingVO exist = sessionMappingMapperCustom.selectBySessionId(sessionMapping.getSessionId());
        if (exist == null) {
            return Result.error("会话映射不存在");
        }
        // 4. 补全更新时间
        sessionMapping.setUpdateTime(new Date());
        // 5. 执行更新
        int update = sessionMappingMapperCustom.updateSessionMapping(sessionMapping);

        // ========== 新增：联动更新登报申请的service_staff_id ==========
        if (update > 0 && StringUtils.hasText(exist.getAppId())) {
            NewspaperApplicationVO application = newspaperApplicationMapperCustom.selectByAppId(exist.getAppId());
            if (application != null) {
                application.setServiceStaffId(sessionMapping.getServiceStaffId());
                application.setUpdateTime(new Date());
                newspaperApplicationMapperCustom.updateStatusByAppId(application);
            }
        }

        // 6. 刷新ReceiverId
        receiverIdService.refreshReceiverIdExpire(receiverId);
        return update > 0 ? Result.success(true) : Result.error("更新失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> deleteByAppId(String appId, String receiverId) {
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或过期");
        }
        if (!StringUtils.hasText(appId)) {
            return Result.paramError("appId不能为空");
        }
        SessionMappingVO exist = sessionMappingMapperCustom.selectByAppId(appId);
        if (exist == null) {
            return Result.error("会话映射不存在");
        }
        int delete = sessionMappingMapperCustom.deleteByAppId(appId);
        receiverIdService.refreshReceiverIdExpire(receiverId);
        return delete > 0 ? Result.success(true) : Result.error("删除失败");
    }
}