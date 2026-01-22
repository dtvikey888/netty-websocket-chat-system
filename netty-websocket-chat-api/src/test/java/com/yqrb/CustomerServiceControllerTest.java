package com.yqrb;

import com.yqrb.controller.CustomerServiceController;
import com.yqrb.pojo.vo.CustomerServiceVO;
import com.yqrb.pojo.vo.ReceiverIdSessionVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.ReceiverIdService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 客服信息接口全量测试
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
@Rollback(true)
public class CustomerServiceControllerTest {

    @Autowired
    private CustomerServiceController customerServiceController;

    @Autowired
    private ReceiverIdService receiverIdService;

    // 通用生成测试ReceiverId
    private String getTestReceiverId(String userId, String userName) {
        ReceiverIdSessionVO session = receiverIdService.generateReceiverId(userId, userName);
        return session.getReceiverId();
    }

    /**
     * 测试：新增客服 /newspaper/customer/add
     */
    @Test
    public void testAddCustomerService() {
        // 1. 构建参数（管理员ReceiverId）
        String adminReceiverId = getTestReceiverId("admin_001", "系统管理员");
        CustomerServiceVO csVO = new CustomerServiceVO();
        csVO.setServiceStaffId("cs_test_001");
        csVO.setServiceName("测试客服");
        csVO.setServicePhone("13700137000");

        // 2. 调用接口
        Result<Boolean> result = customerServiceController.addCustomerService(csVO, adminReceiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "新增客服返回码非200";
        assert result.getData() == true : "新增客服失败";
        System.out.println("新增客服接口测试通过");
    }

    /**
     * 测试：查询客服详情 /newspaper/customer/detail/{serviceStaffId}
     */
    @Test
    public void testGetCustomerDetail() {
        // 1. 先新增一个测试客服
        testAddCustomerService();
        String receiverId = getTestReceiverId("user_001", "测试用户1");
        String serviceStaffId = "cs_test_001";

        // 2. 调用接口
        Result<CustomerServiceVO> result = customerServiceController.getCustomerDetail(serviceStaffId, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "查询客服详情返回码非200";
        assert result.getData() != null : "客服详情为空";
        assert result.getData().getServiceStaffId().equals("cs_test_001") : "客服ID不一致";
        System.out.println("查询客服详情接口测试通过");
    }

    /**
     * 测试：查询所有在线客服 /newspaper/customer/list/online
     */
    @Test
    public void testGetOnlineCustomerList() {
        // 1. 构建参数
        String receiverId = getTestReceiverId("user_001", "测试用户1");

        // 2. 调用接口
        Result<List<CustomerServiceVO>> result = customerServiceController.getOnlineCustomerList(receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "查询在线客服返回码非200";
        assert result.getData() != null : "在线客服列表为空";
        System.out.println("查询所有在线客服接口测试通过，共" + result.getData().size() + "名在线客服");
    }

    /**
     * 测试：查询所有客服 /newspaper/customer/list/all
     */
    @Test
    public void testGetAllCustomerList() {
        // 1. 构建参数
        String receiverId = getTestReceiverId("user_001", "测试用户1");

        // 2. 调用接口
        Result<List<CustomerServiceVO>> result = customerServiceController.getAllCustomerList(receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "查询所有客服返回码非200";
        assert result.getData() != null && !result.getData().isEmpty() : "客服列表为空";
        System.out.println("查询所有客服接口测试通过，共" + result.getData().size() + "名客服");
    }

    /**
     * 测试：客服登录 /newspaper/customer/login/{serviceStaffId}
     */
    @Test
    public void testCustomerLogin() {
        // 1. 先新增测试客服
        testAddCustomerService();
        String receiverId = getTestReceiverId("cs_test_001", "测试客服");
        String serviceStaffId = "cs_test_001";

        // 2. 调用接口
        Result<Boolean> result = customerServiceController.customerLogin(serviceStaffId, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "客服登录返回码非200";
        assert result.getData() == true : "客服登录失败";
        System.out.println("客服登录接口测试通过");
    }

    /**
     * 测试：客服登出 /newspaper/customer/logout/{serviceStaffId}
     */
    @Test
    public void testCustomerLogout() {
        // 1. 先让客服登录
        testCustomerLogin();
        String receiverId = getTestReceiverId("cs_test_001", "测试客服");
        String serviceStaffId = "cs_test_001";

        // 2. 调用接口
        Result<Boolean> result = customerServiceController.customerLogout(serviceStaffId, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "客服登出返回码非200";
        assert result.getData() == true : "客服登出失败";
        System.out.println("客服登出接口测试通过");
    }
}