package com.yqrb;

import com.yqrb.controller.SessionMappingController;
import com.yqrb.pojo.vo.ReceiverIdSessionVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.pojo.vo.SessionMappingVO;
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
 * 会话映射接口全量测试
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
@Rollback(true)
public class SessionMappingControllerTest {

    @Autowired
    private SessionMappingController sessionMappingController;

    @Autowired
    private ReceiverIdService receiverIdService;



    // 通用生成测试ReceiverId
    private String getTestReceiverId(String userId, String userName) {
        ReceiverIdSessionVO session = receiverIdService.generateReceiverId(userId, userName);
        return session.getReceiverId();
    }

    /**
     * 测试：创建会话映射 /yqrb/session/create
     */
    @Test
    public void testCreateSessionMapping() {
        // 1. 构建参数
        String receiverId = getTestReceiverId("user_001", "测试用户1");
        SessionMappingVO sessionVO = new SessionMappingVO();
        sessionVO.setSessionId("SESSION_TEST_001");
        sessionVO.setUserId("user_001");
        sessionVO.setAppId("APP_TEST_001");
        sessionVO.setServiceStaffId("cs_001");

        // 2. 调用接口
        Result<SessionMappingVO> result = sessionMappingController.createSessionMapping(sessionVO, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "创建会话映射返回码非200";
        assert result.getData() != null : "会话映射返回数据为空";
        assert result.getData().getSessionId().equals("SESSION_TEST_001") : "会话ID不一致";
        System.out.println("创建会话映射接口测试通过");
    }

    /**
     * 测试：按会话ID查询映射 /yqrb/session/session/{sessionId}
     */
    @Test
    public void testGetBySessionId() {
        // 1. 先创建测试会话映射
        testCreateSessionMapping();
        String receiverId = getTestReceiverId("user_001", "测试用户1");
        String sessionId = "SESSION_TEST_001";

        // 2. 调用接口
        Result<SessionMappingVO> result = sessionMappingController.getBySessionId(sessionId, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "按会话ID查询映射返回码非200";
        assert result.getData() != null : "会话映射为空";
        assert result.getData().getSessionId().equals(sessionId) : "会话ID不一致";
        System.out.println("按会话ID查询映射接口测试通过");
    }

    /**
     * 测试：按申请ID查询映射 /yqrb/session/app/{appId}
     */
    @Test
    public void testGetByAppId() {
        // 1. 先创建测试会话映射
        testCreateSessionMapping();
        String receiverId = getTestReceiverId("user_001", "测试用户1");
        String appId = "APP_TEST_001";

        // 2. 调用接口
        Result<SessionMappingVO> result = sessionMappingController.getByAppId(appId, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "按申请ID查询映射返回码非200";
        assert result.getData() != null : "会话映射为空";
        assert result.getData().getAppId().equals(appId) : "申请ID不一致";
        System.out.println("按申请ID查询映射接口测试通过");
    }

    /**
     * 测试：按用户ID查询会话列表 /yqrb/session/user/{userId}
     */
    @Test
    public void testGetByUserId() {
        // 1. 先创建测试会话映射
        testCreateSessionMapping();
        String receiverId = getTestReceiverId("user_001", "测试用户1");
        String userId = "user_001";

        // 2. 调用接口
        Result<List<SessionMappingVO>> result = sessionMappingController.getByUserId(userId, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "按用户ID查询会话列表返回码非200";
        assert result.getData() != null && !result.getData().isEmpty() : "用户会话列表为空";
        System.out.println("按用户ID查询会话列表接口测试通过，共" + result.getData().size() + "个会话");
    }

    /**
     * 测试：按客服ID查询承接的会话列表 /yqrb/session/service/{serviceStaffId}
     */
    @Test
    public void testGetByServiceStaffId() {
        // 1. 先创建测试会话映射
        testCreateSessionMapping();
        String receiverId = getTestReceiverId("cs_001", "测试客服");
        String serviceStaffId = "cs_001";

        // 2. 调用接口
        Result<List<SessionMappingVO>> result = sessionMappingController.getByServiceStaffId(serviceStaffId, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "按客服ID查询会话列表返回码非200";
        assert result.getData() != null : "客服会话列表为空";
        System.out.println("按客服ID查询承接会话列表接口测试通过，共" + result.getData().size() + "个会话");
    }

    /**
     * 测试：按用户+申请ID查询会话 /yqrb/session/user-app
     */
    @Test
    public void testGetByUserIdAndAppId() {
        // 1. 先创建测试会话映射
        testCreateSessionMapping();
        String receiverId = getTestReceiverId("user_001", "测试用户1");
        String userId = "user_001";
        String appId = "APP_TEST_001";

        // 2. 调用接口
        Result<SessionMappingVO> result = sessionMappingController.getByUserIdAndAppId(userId, appId, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "按用户+申请ID查询会话返回码非200";
        assert result.getData() != null : "会话映射为空";
        assert result.getData().getUserId().equals(userId) && result.getData().getAppId().equals(appId) : "用户/申请ID不一致";
        System.out.println("按用户+申请ID查询会话接口测试通过");
    }

    /**
     * 测试：更新会话映射 /yqrb/session/update
     */
    @Test
    public void testUpdateSessionMapping() {
        // 1. 先创建测试会话映射
        testCreateSessionMapping();
        String receiverId = getTestReceiverId("user_001", "测试用户1");
        SessionMappingVO updateVO = new SessionMappingVO();
        updateVO.setSessionId("SESSION_TEST_001");
        updateVO.setServiceStaffId("cs_002"); // 更换客服

        // 2. 调用接口
        Result<Boolean> result = sessionMappingController.updateSessionMapping(updateVO, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "更新会话映射返回码非200";
        assert result.getData() == true : "更新会话映射失败";
        System.out.println("更新会话映射接口测试通过");
    }

    /**
     * 测试：按会话ID删除映射 /yqrb/session/delete/{sessionId}
     */
    @Test
    public void testDeleteBySessionId() {
        // 1. 先创建测试会话映射
        testCreateSessionMapping();
        String receiverId = getTestReceiverId("user_001", "测试用户1");
        String sessionId = "SESSION_TEST_001";

        // 2. 调用接口
        Result<Boolean> result = sessionMappingController.deleteBySessionId(sessionId, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "删除会话映射返回码非200";
        assert result.getData() == true : "删除会话映射失败";
        System.out.println("按会话ID删除映射接口测试通过");
    }

    /**
     * 测试：按申请ID删除会话映射 /yqrb/session/delete/app/{appId}
     */
    @Test
    public void testDeleteByAppId() {
        // 1. 先创建测试会话映射
        testCreateSessionMapping();
        String receiverId = getTestReceiverId("user_001", "测试用户1");
        String appId = "APP_TEST_001";

        // 2. 调用接口
        Result<Boolean> result = sessionMappingController.deleteByAppId(appId, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "按申请ID删除映射返回码非200";
        assert result.getData() == true : "删除映射失败";
        System.out.println("按申请ID删除会话映射接口测试通过");
    }
}