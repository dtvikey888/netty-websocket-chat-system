package com.yqrb.service;

import com.yqrb.pojo.vo.CustomerServiceVO;
import com.yqrb.pojo.vo.Result;
import java.util.List;

public interface CustomerServiceService {
    // 新增客服（管理员接口）
    Result<Boolean> addCustomerService(CustomerServiceVO customerServiceVO, String receiverId);

    // 根据客服ID查询详情
    Result<CustomerServiceVO> getCustomerByStaffId(String serviceStaffId, String receiverId);

    // 查询所有在线客服
    Result<List<CustomerServiceVO>> getOnlineCustomerList(String receiverId);

    // 查询所有客服
    Result<List<CustomerServiceVO>> getAllCustomerList(String receiverId);

    // 客服登录（更新在线状态）
    Result<Boolean> customerLogin(String serviceStaffId, String receiverId);

    // 客服登出（更新离线状态）
    Result<Boolean> customerLogout(String serviceStaffId, String receiverId);
}