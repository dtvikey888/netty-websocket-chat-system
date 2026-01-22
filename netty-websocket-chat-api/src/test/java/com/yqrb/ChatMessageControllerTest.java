package com.yqrb;

import com.yqrb.controller.ChatMessageController;
import com.yqrb.pojo.vo.ChatMessageVO;
import com.yqrb.pojo.vo.ReceiverIdSessionVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.pojo.vo.WebSocketMsgVO;
import com.yqrb.service.ReceiverIdService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 聊天消息接口全量测试（已修正VO字段匹配问题）
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional // 测试后回滚数据
@Rollback(true)
public class ChatMessageControllerTest {

    @Autowired
    private ChatMessageController chatMessageController;

    @Autowired
    private ReceiverIdService receiverIdService;

    // 通用生成测试ReceiverId
    private String getTestReceiverId(String userId, String userName) {
        ReceiverIdSessionVO session = receiverIdService.generateReceiverId(userId, userName);
        return session.getReceiverId();
    }

    /**
     * 测试：发送聊天消息接口 /newspaper/chat/send
     * 修正点：适配WebSocketMsgVO的真实字段（msgContent/userId 而非 content/fromUserId）
     */
    @Test
    public void testSendMessage() {
        // 1. 构建参数（严格匹配WebSocketMsgVO的字段）
        String receiverId = getTestReceiverId("user_001", "测试用户1");
        WebSocketMsgVO webSocketMsg = new WebSocketMsgVO();
        webSocketMsg.setSessionId("SESSION_TEST_001");
        webSocketMsg.setMsgContent("测试聊天消息内容"); // 修正：msgContent 而非 content
        webSocketMsg.setUserId("user_001"); // 修正：userId 而非 fromUserId
        webSocketMsg.setMsgType("text"); // 补充：必填的消息类型
        webSocketMsg.setSendTime(new Date()); // 补充：发送时间

        // 2. 调用接口
        Result<ChatMessageVO> result = chatMessageController.sendMessage(webSocketMsg, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "发送消息接口返回码非200";
        assert result.getData() != null : "发送消息接口返回数据为空";
        assert result.getData().getContent().equals("测试聊天消息内容") : "消息内容不一致";
        System.out.println("发送聊天消息接口测试通过");
    }

    /**
     * 测试：查询会话消息列表 /newspaper/chat/list/{sessionId}
     */
    @Test
    public void testGetMessageList() {
        // 1. 构建参数
        String receiverId = getTestReceiverId("user_001", "测试用户1");
        String sessionId = "SESSION_TEST_001";

        // 2. 先发送一条消息（保证有数据）
        testSendMessage();

        // 3. 调用接口
        Result<List<ChatMessageVO>> result = chatMessageController.getMessageList(sessionId, receiverId);

        // 4. 断言结果
        assert result.getCode() == 200 : "查询会话消息列表返回码非200";
        assert result.getData() != null && !result.getData().isEmpty() : "会话消息列表为空";
        System.out.println("查询会话消息列表接口测试通过，共" + result.getData().size() + "条消息");
    }

    /**
     * 测试：查询未读消息列表 /newspaper/chat/unread
     */
    @Test
    public void testGetUnreadMessageList() {
        // 1. 构建参数
        String receiverId = getTestReceiverId("user_001", "测试用户1");

        // 2. 先发送一条未读消息（保证有数据）
        testSendMessage();

        // 3. 调用接口
        Result<List<ChatMessageVO>> result = chatMessageController.getUnreadMessageList(receiverId);

        // 4. 断言结果
        assert result.getCode() == 200 : "查询未读消息列表返回码非200";
        assert result.getData() != null : "未读消息列表返回为空";
        System.out.println("查询未读消息列表接口测试通过，共" + result.getData().size() + "条未读消息");
    }

    /**
     * 测试：标记消息为已读 /newspaper/chat/read/{msgId}
     */
    @Test
    public void testMarkMsgAsRead() {
        // 1. 先发送一条消息（获取msgId）
        Result<ChatMessageVO> sendResult = testSendMessageReturnResult();
        String msgId = sendResult.getData().getMsgId();
        String receiverId = getTestReceiverId("user_001", "测试用户1");

        // 2. 调用接口
        Result<Boolean> result = chatMessageController.markMsgAsRead(msgId, receiverId);

        // 3. 断言结果
        assert result.getCode() == 200 : "标记消息已读返回码非200";
        assert result.getData() == true : "标记消息已读失败";
        System.out.println("标记消息为已读接口测试通过");
    }

    /**
     * 测试：删除会话所有消息 /newspaper/chat/delete/session/{sessionId}
     */
    @Test
    public void testDeleteMessageBySessionId() {
        // 1. 构建参数
        String receiverId = getTestReceiverId("user_001", "测试用户1");
        String sessionId = "SESSION_TEST_001";

        // 2. 先发送一条消息（保证有可删除的数据）
        testSendMessage();

        // 3. 调用接口
        Result<Boolean> result = chatMessageController.deleteMessageBySessionId(sessionId, receiverId);

        // 4. 断言结果
        assert result.getCode() == 200 : "删除会话消息返回码非200";
        assert result.getData() == true : "删除会话消息失败";
        System.out.println("删除会话所有消息接口测试通过");
    }

    // 辅助方法：发送消息并返回结果（用于获取msgId）- 修正字段匹配问题
    private Result<ChatMessageVO> testSendMessageReturnResult() {
        String receiverId = getTestReceiverId("user_001", "测试用户1");
        WebSocketMsgVO webSocketMsg = new WebSocketMsgVO();
        webSocketMsg.setSessionId("SESSION_TEST_001");
        webSocketMsg.setMsgContent("测试未读消息"); // 修正：msgContent
        webSocketMsg.setUserId("user_001"); // 修正：userId
        webSocketMsg.setMsgType("text");
        webSocketMsg.setSendTime(new Date());
        return chatMessageController.sendMessage(webSocketMsg, receiverId);
    }
}