package com.yqrb;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * 数据访问层单元测试（测试各Mapper的SQL执行有效性）
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class MapperLayerTest {

    @Autowired
    private CustomerServiceMapperCustom customerServiceMapperCustom;

    /**
     * 测试：CustomerServiceMapper相关方法
     */
    @Test
    public void testCustomerServiceMapper() {
        // 1. 查询所有在线客服
        List<CustomerServiceVO> onlineCustomerList = customerServiceMapperCustom.selectOnlineCustomer(CustomerServiceVO.STATUS_ONLINE);
        System.out.println("在线客服数量：" + onlineCustomerList.size());
        onlineCustomerList.forEach(cs -> System.out.println("在线客服：" + cs.getServiceStaffId() + " - " + cs.getServiceName()));

        // 2. 查询单个客服
        CustomerServiceVO customerService = customerServiceMapperCustom.selectByServiceStaffId("cs_001");
        System.out.println("查询客服cs_001：" + customerService);

        // 3. 更新客服状态（测试XML映射方法）
        if (customerService != null) {
            customerService.setStatus(CustomerServiceVO.STATUS_OFFLINE);
            customerService.setLoginTime(DateUtil.getCurrentDate());
            customerService.setUpdateTime(DateUtil.getCurrentDate());
            int updateResult = customerServiceMapperCustom.updateCustomerStatus(customerService);
            System.out.println("更新客服状态结果：" + updateResult);

            // 还原客服状态（避免影响其他测试）
            customerService.setStatus(CustomerServiceVO.STATUS_ONLINE);
            customerServiceMapperCustom.updateCustomerStatus(customerService);
        }

        // 4. 断言结果
        assert onlineCustomerList != null && !onlineCustomerList.isEmpty();
        assert customerService != null;
        assert "cs_001".equals(customerService.getServiceStaffId());
        System.out.println("CustomerServiceMapper单元测试通过");
    }
}