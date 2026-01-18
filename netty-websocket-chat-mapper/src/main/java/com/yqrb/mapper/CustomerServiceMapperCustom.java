package com.yqrb.mapper;

import com.yqrb.pojo.vo.CustomerServiceVO;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerServiceMapperCustom {

    // 根据客服唯一标识查询信息
    @Select("SELECT * FROM customer_service WHERE service_staff_id = #{serviceStaffId}")
    CustomerServiceVO selectByServiceStaffId(String serviceStaffId);

    // 查询所有在线客服
    @Select("SELECT * FROM customer_service WHERE status = #{status} ORDER BY create_time ASC")
    List<CustomerServiceVO> selectOnlineCustomer(String status);

    // 查询所有客服
    @Select("SELECT * FROM customer_service ORDER BY create_time ASC")
    List<CustomerServiceVO> selectAllCustomer();

    // 新增客服（管理员接口）
    int insertCustomerService(CustomerServiceVO customerService);

    // 更新客服状态和最后登录时间（XML实现复杂SQL）
    int updateCustomerStatus(CustomerServiceVO customerService);
}