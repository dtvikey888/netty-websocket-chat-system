package com.yqrb.service;

import com.yqrb.pojo.vo.NewspaperApplicationVO;
import com.yqrb.pojo.vo.Result;

import java.math.BigDecimal;
import java.util.List;

public interface NewspaperApplicationService {
    // 提交登报申请
    Result<NewspaperApplicationVO> submitApplication(NewspaperApplicationVO application, String receiverId);

    // 根据appId查询申请详情
    Result<NewspaperApplicationVO> getApplicationByAppId(String appId, String receiverId);

    // 根据用户ID查询申请列表
    Result<List<NewspaperApplicationVO>> getApplicationListByUserId(String userId, String receiverId);

    // 根据客服ID查询申请列表
    Result<List<NewspaperApplicationVO>> getApplicationListByServiceStaffId(String serviceStaffId, String receiverId);

    // 审核登报申请
    // ========== 核心修改：新增BigDecimal payAmount参数 ==========
    Result<Boolean> auditApplication(String appId, String status, String auditRemark, BigDecimal payAmount, String receiverId);

    // 删除登报申请（管理员接口）
    Result<Boolean> deleteApplication(String appId, String receiverId);
}