package com.yqrb;

import com.yqrb.pojo.vo.CustomerServiceVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.CustomerServiceService;
import com.yqrb.service.ReceiverIdService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * 业务层单元测试（测试各Service的核心方法）
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class ServiceLayerTest {

    @Autowired
    private ReceiverIdService receiverIdService;

    @Autowired
    private CustomerServiceService customerServiceService;

    /**
     * 测试：ReceiverId相关方法
     */
    @Test
    public void testReceiverIdService() {
        // 1. 生成ReceiverId
        String receiverId = receiverIdService.generateReceiverId("user_002", "测试用户2").getReceiverId();
        System.out.println("生成ReceiverId：" + receiverId);

        // 2. 校验ReceiverId有效性
        boolean isValid = receiverIdService.validateReceiverId(receiverId);
        System.out.println("ReceiverId有效性校验：" + isValid);

        // 3. 刷新过期时间
        boolean isRefreshed = receiverIdService.refreshReceiverIdExpire(receiverId);
        System.out.println("ReceiverId过期时间刷新：" + isRefreshed);

        // 4. 销毁ReceiverId
        boolean isDestroyed = receiverIdService.destroyReceiverId(receiverId);
        System.out.println("ReceiverId销毁：" + isDestroyed);

        // 5. 校验销毁后的有效性
        boolean isValidAfterDestroy = receiverIdService.validateReceiverId(receiverId);
        System.out.println("ReceiverId销毁后有效性校验：" + isValidAfterDestroy);

        // 断言结果
        assert isValid;
        assert isRefreshed;
        assert isDestroyed;
        assert !isValidAfterDestroy;
        System.out.println("ReceiverIdService单元测试通过");
    }

    /**
     * 测试：客服信息相关方法
     */
    @Test
    public void testCustomerServiceService() {
        // 1. 生成测试ReceiverId
        String receiverId = receiverIdService.generateReceiverId("cs_admin", "客服管理员").getReceiverId();

        // 2. 查询在线客服
        Result<List<CustomerServiceVO>> onlineResult = customerServiceService.getOnlineCustomerList(receiverId);
        System.out.println("在线客服列表：" + onlineResult);

        // 3. 查询单个客服详情
        Result<CustomerServiceVO> detailResult = customerServiceService.getCustomerByStaffId("cs_001", receiverId);
        System.out.println("客服cs_001详情：" + detailResult);

        // 4. 断言结果
        assert onlineResult.getCode() == 200;
        assert onlineResult.getData() != null && !onlineResult.getData().isEmpty();
        assert detailResult.getCode() == 200;
        assert detailResult.getData() != null && "cs_001".equals(detailResult.getData().getServiceStaffId());
        System.out.println("CustomerServiceService单元测试通过");
    }
}