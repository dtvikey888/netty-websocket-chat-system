package com.yqrb.service.impl;

import com.yqrb.mapper.CustomerServiceMapperCustom;
import com.yqrb.pojo.vo.CustomerServiceVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.CustomerServiceService;
import com.yqrb.service.ReceiverIdService;
import com.yqrb.util.DateUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class CustomerServiceServiceImpl implements CustomerServiceService {

    @Resource
    private CustomerServiceMapperCustom customerServiceMapperCustom;

    @Resource
    private ReceiverIdService receiverIdService;

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