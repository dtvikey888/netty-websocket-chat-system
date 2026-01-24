package com.yqrb;

import com.yqrb.controller.NewspaperApplicationController;
import com.yqrb.pojo.vo.NewspaperApplicationVO;
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

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 登报申请接口全量测试
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
@Rollback(true)
public class NewspaperApplicationControllerTest {

    @Autowired
    private NewspaperApplicationController newspaperApplicationController;

    @Autowired
    private ReceiverIdService receiverIdService;

    // 通用生成测试ReceiverId
    private String getTestReceiverId(String userId, String userName) {
        ReceiverIdSessionVO session = receiverIdService.generateReceiverId(userId, userName);
        return session.getReceiverId();
    }

    /**
     * 测试：提交登报申请 /newspaper/application/submit
     */
    @Test
    public void testSubmitApplication() {
        // 1. 构建参数
        String receiverId = getTestReceiverId("user_001", "测试用户1");
        NewspaperApplicationVO appVO = new NewspaperApplicationVO();
        appVO.setUserId("user_001");
        appVO.setServiceStaffId("cs_001");
        appVO.setUserName("测试用户");
        appVO.setUserPhone("13800138000");
        appVO.setCertType("营业执照遗失登报");
        appVO.setSealRe("no");
        appVO.setPayAmount(new BigDecimal("199.00"));
        appVO.setSubmitTime(new Date());
        appVO.setCreateTime(new Date());
        appVO.setUpdateTime(new Date());

        // 2. 调用接口
        Result<NewspaperApplicationVO> result = newspaperApplicationController.submitApplication(appVO, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "提交登报申请返回码非200";
        assert result.getData() != null : "申请返回数据为空";
        assert result.getData().getAppId() != null : "申请ID为空";
        System.out.println("提交登报申请接口测试通过，申请ID：" + result.getData().getAppId());
    }

    /**
     * 测试：查询申请详情 /newspaper/application/detail/{appId}
     */
    @Test
    public void testGetAppDetail() {
        // 1. 先提交测试申请
        Result<NewspaperApplicationVO> submitResult = testSubmitApplicationReturnResult();
        String appId = submitResult.getData().getAppId();
        String receiverId = getTestReceiverId("user_001", "测试用户1");

        // 2. 调用接口
        Result<NewspaperApplicationVO> result = newspaperApplicationController.getAppDetail(appId, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "查询申请详情返回码非200";
        assert result.getData() != null : "申请详情为空";
        assert result.getData().getAppId().equals(appId) : "申请ID不一致";
        System.out.println("查询登报申请详情接口测试通过");
    }

    /**
     * 测试：查询用户申请列表 /newspaper/application/list/user/{userId}
     */
    @Test
    public void testGetAppListByUser() {
        // 1. 先提交测试申请
        testSubmitApplication();
        String receiverId = getTestReceiverId("user_001", "测试用户1");
        String userId = "user_001";

        // 2. 调用接口
        Result<List<NewspaperApplicationVO>> result = newspaperApplicationController.getAppListByUser(userId, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "查询用户申请列表返回码非200";
        assert result.getData() != null && !result.getData().isEmpty() : "用户申请列表为空";
        System.out.println("查询用户登报申请列表接口测试通过，共" + result.getData().size() + "条申请");
    }

    /**
     * 测试：查询客服处理申请列表 /newspaper/application/list/cs/{serviceStaffId}
     */
    @Test
    public void testGetAppListByCs() {
        // 1. 先提交测试申请
        testSubmitApplication();
        String receiverId = getTestReceiverId("cs_001", "测试客服");
        String serviceStaffId = "cs_001";

        // 2. 调用接口
        Result<List<NewspaperApplicationVO>> result = newspaperApplicationController.getAppListByCs(serviceStaffId, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "查询客服处理申请列表返回码非200";
        assert result.getData() != null : "客服申请列表为空";
        System.out.println("查询客服处理申请列表接口测试通过，共" + result.getData().size() + "条申请");
    }

    /**
     * 测试：审核登报申请 /newspaper/application/audit
     */
//    @Test
//    public void testAuditApp() {
//        // 1. 先提交测试申请
//        Result<NewspaperApplicationVO> submitResult = testSubmitApplicationReturnResult();
//        String appId = submitResult.getData().getAppId();
//        String receiverId = getTestReceiverId("cs_001", "测试客服");
//        String status = "approved"; // 审核通过
//        String auditRemark = "审核通过，符合登报条件";
//
//        // 2. 调用接口
//        Result<Boolean> result = newspaperApplicationController.auditApp(appId, status, auditRemark, receiverId);
//
//        // 3. 断言结果
//        assert result.getCode() == 200 : "审核登报申请返回码非200";
//        assert result.getData() == true : "审核申请失败";
//        System.out.println("审核登报申请接口测试通过");
//    }

    /**
     * 测试：删除登报申请 /newspaper/application/delete/{appId}
     */
    @Test
    public void testDeleteApp() {
        // 1. 先提交测试申请
        Result<NewspaperApplicationVO> submitResult = testSubmitApplicationReturnResult();
        String appId = submitResult.getData().getAppId();
        String receiverId = getTestReceiverId("user_001", "测试用户1");

        // 2. 调用接口
        Result<Boolean> result = newspaperApplicationController.deleteApp(appId, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "删除登报申请返回码非200";
        assert result.getData() == true : "删除申请失败";
        System.out.println("删除登报申请接口测试通过");
    }

    // 辅助方法：提交申请并返回结果（用于获取appId）
    private Result<NewspaperApplicationVO> testSubmitApplicationReturnResult() {
        String receiverId = getTestReceiverId("user_001", "测试用户1");
        NewspaperApplicationVO appVO = new NewspaperApplicationVO();
        appVO.setUserId("user_001");
        appVO.setServiceStaffId("cs_001");
        appVO.setUserName("测试用户");
        appVO.setUserPhone("13800138000");
        appVO.setCertType("营业执照遗失登报");
        appVO.setSealRe("no");
        appVO.setPayAmount(new BigDecimal("199.00"));
        appVO.setSubmitTime(new Date());
        appVO.setCreateTime(new Date());
        appVO.setUpdateTime(new Date());
        return newspaperApplicationController.submitApplication(appVO, receiverId);
    }
}