package com.yqrb;

import com.yqrb.pojo.vo.CustomerServiceVO;
import com.yqrb.pojo.vo.NewspaperApplicationVO;
import com.yqrb.pojo.vo.ReceiverIdSessionVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.CustomerServiceService;
import com.yqrb.service.NewspaperApplicationService;
import com.yqrb.service.ReceiverIdService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 系统整体集成测试（测试核心业务流程闭环）
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = NewspaperApplicationVO.class)
public class SystemIntegratedTest {

    @Autowired
    private ReceiverIdService receiverIdService;

    @Autowired
    private NewspaperApplicationService newspaperApplicationService;

    @Autowired
    private CustomerServiceService customerServiceService;

    // 新增测试：新增客服
    @Test
    public void testAddCustomerService() {
        // 1. 生成管理员ReceiverId
        String adminReceiverId = receiverIdService.generateReceiverId("admin_001", "系统管理员").getReceiverId();
        // 2. 构建测试VO
        CustomerServiceVO testVO = new CustomerServiceVO("cs_003", "客服小王", "13700137000");
        // 3. 调用新增接口
        Result<Boolean> result = customerServiceService.addCustomerService(testVO, adminReceiverId);
        // 4. 断言结果
        assert result.getCode() == 200;
        assert result.getData() == true;
        System.out.println("新增客服测试通过");
    }

    /**
     * 测试：生成ReceiverId + 提交登报申请 完整流程
     */
    @Test
    public void testCompleteBusinessFlow() {
        // 1. 生成测试ReceiverId
        String testUserId = "user_001";
        String testUserName = "测试用户";
        ReceiverIdSessionVO receiverSession = receiverIdService.generateReceiverId(testUserId, testUserName);
        String receiverId = receiverSession.getReceiverId();
        System.out.println("生成测试ReceiverId：" + receiverId);

        // 2. 构建测试登报申请
        NewspaperApplicationVO testApp = new NewspaperApplicationVO();
        testApp.setUserId(testUserId);
        testApp.setServiceStaffId("cs_001");
        testApp.setUserName(testUserName);
        testApp.setUserPhone("13800138000");
        testApp.setCertType("营业执照遗失登报");
        testApp.setSealRe("no");
        testApp.setPayAmount(new BigDecimal("199.00"));
        testApp.setSubmitTime(new Date());
        testApp.setCreateTime(new Date());
        testApp.setUpdateTime(new Date());

        // 3. 提交登报申请
        Result<NewspaperApplicationVO> result = newspaperApplicationService.submitApplication(testApp, receiverId);
        System.out.println("登报申请提交结果：" + result);

        // 4. 校验结果
        assert result.getCode() == 200;
        assert result.getData() != null;
        assert result.getData().getAppId() != null;
        System.out.println("整体业务流程测试通过，申请ID：" + result.getData().getAppId());
    }
}