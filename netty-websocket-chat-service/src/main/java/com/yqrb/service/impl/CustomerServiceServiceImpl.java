package com.yqrb.service.impl;

import com.yqrb.mapper.CustomerServiceMapperCustom;
import com.yqrb.pojo.vo.CustomerServiceVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.CustomerServiceService;
import com.yqrb.service.ReceiverIdService;
import com.yqrb.util.DateUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

@Service
public class CustomerServiceServiceImpl implements CustomerServiceService {

    @Resource
    private CustomerServiceMapperCustom customerServiceMapperCustom;

    @Resource
    private ReceiverIdService receiverIdService;

    // 新增客服（管理员接口，实现业务逻辑）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> addCustomerService(CustomerServiceVO customerServiceVO, String receiverId) {
        // 1. 校验ReceiverId有效性（仅管理员有效，此处复用现有校验逻辑）
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期，无新增客服权限");
        }

        // 2. 校验VO参数完整性
        if (!StringUtils.hasText(customerServiceVO.getServiceStaffId()) || !StringUtils.hasText(customerServiceVO.getServiceName())) {
            return Result.paramError("客服唯一标识（serviceStaffId）和客服姓名（serviceName）不能为空");
        }

        // 3. 校验客服是否已存在（避免重复新增）
        CustomerServiceVO existingCustomer = customerServiceMapperCustom.selectByServiceStaffId(customerServiceVO.getServiceStaffId());
        if (existingCustomer != null) {
            return Result.error("该客服已存在，请勿重复新增（serviceStaffId：" + customerServiceVO.getServiceStaffId() + "）");
        }

        // 4. 调用Mapper执行新增操作
        int insertResult = customerServiceMapperCustom.insertCustomerService(customerServiceVO);
        if (insertResult <= 0) {
            return Result.error("新增客服失败，请重试");
        }

        // 5. 刷新ReceiverId过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);

        // 6. 返回成功结果
        return Result.success(true);
    }

    @Override
    public Result<CustomerServiceVO> getCustomerByStaffId(String serviceStaffId, String receiverId) {
        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 2. 查询客服信息
        CustomerServiceVO customerService = customerServiceMapperCustom.selectByServiceStaffId(serviceStaffId);
        if (customerService == null) {
            return Result.error("客服信息不存在");
        }

        // 3. 刷新ReceiverId过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);

        return Result.success(customerService);
    }

    @Override
    public Result<List<CustomerServiceVO>> getOnlineCustomerList(String receiverId) {
        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 2. 查询在线客服
        List<CustomerServiceVO> customerList = customerServiceMapperCustom.selectOnlineCustomer(CustomerServiceVO.STATUS_ONLINE);

        // 3. 刷新ReceiverId过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);

        return Result.success(customerList);
    }

    @Override
    public Result<List<CustomerServiceVO>> getAllCustomerList(String receiverId) {
        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 2. 查询所有客服
        List<CustomerServiceVO> customerList = customerServiceMapperCustom.selectAllCustomer();

        // 3. 刷新ReceiverId过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);

        return Result.success(customerList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> customerLogin(String serviceStaffId, String receiverId) {
        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 2. 查询客服信息
        CustomerServiceVO customerService = customerServiceMapperCustom.selectByServiceStaffId(serviceStaffId);
        if (customerService == null) {
            return Result.error("客服信息不存在");
        }

        // 3. 更新客服状态为在线
        customerService.setStatus(CustomerServiceVO.STATUS_ONLINE);
        customerService.setLoginTime(DateUtil.getCurrentDate());
        customerService.setUpdateTime(DateUtil.getCurrentDate());

        int updateResult = customerServiceMapperCustom.updateCustomerStatus(customerService);
        if (updateResult <= 0) {
            return Result.error("客服登录失败");
        }

        // 4. 刷新ReceiverId过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);

        return Result.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> customerLogout(String serviceStaffId, String receiverId) {
        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 2. 查询客服信息
        CustomerServiceVO customerService = customerServiceMapperCustom.selectByServiceStaffId(serviceStaffId);
        if (customerService == null) {
            return Result.error("客服信息不存在");
        }

        // 3. 更新客服状态为离线
        customerService.setStatus(CustomerServiceVO.STATUS_OFFLINE);
        customerService.setUpdateTime(DateUtil.getCurrentDate());

        int updateResult = customerServiceMapperCustom.updateCustomerStatus(customerService);
        if (updateResult <= 0) {
            return Result.error("客服登出失败");
        }

        // 4. 刷新ReceiverId过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);

        return Result.success(true);
    }
}