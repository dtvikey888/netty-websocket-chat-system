package com.yqrb.controller;

import com.yqrb.pojo.vo.ChatMessageVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.pojo.vo.WebSocketMsgVO;
import com.yqrb.service.ChatMessageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/newspaper/chat")
@Api(tags = "聊天消息接口")
public class ChatMessageController {

    @Resource
    private ChatMessageService chatMessageService;

    @PostMapping("/send")
    @ApiOperation("发送聊天消息")
    public Result<ChatMessageVO> sendMessage(
            @RequestBody WebSocketMsgVO webSocketMsg,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return chatMessageService.sendMessage(webSocketMsg, receiverId);
    }

    @GetMapping("/list/{sessionId}")
    @ApiOperation("查询会话消息列表")
    public Result<List<ChatMessageVO>> getMessageList(
            @PathVariable String sessionId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return chatMessageService.getMessageListBySessionId(sessionId, receiverId);
    }

    @GetMapping("/unread")
    @ApiOperation("查询未读消息列表")
    public Result<List<ChatMessageVO>> getUnreadMessageList(
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return chatMessageService.getUnreadMessageList(receiverId);
    }

    @PutMapping("/read/{msgId}")
    @ApiOperation("标记消息为已读")
    public Result<Boolean> markMsgAsRead(
            @PathVariable String msgId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return chatMessageService.markMsgAsRead(msgId, receiverId);
    }

    // 新增：按sessionId删除会话所有消息
    @DeleteMapping("/delete/session/{sessionId}")
    @ApiOperation("删除会话所有消息（按sessionId）")
    public Result<Boolean> deleteMessageBySessionId(
            @PathVariable String sessionId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return chatMessageService.deleteMessageBySessionId(sessionId, receiverId);
    }

    /**
     * 整体验证与注意事项
     * 依赖检查：确保SpringContextUtil能正常获取ChatMessageService，无 Bean 注入异常。
     * 日志验证：启动服务后，用户 / 客服上线，查看日志是否有「未读消息推送准备」「未读消息推送完成」的打印。
     * 前端对接：
     * 上线推送的未读消息格式和普通消息一致，前端无需额外修改渲染逻辑。
     * 进入会话时调用/newspaper/chat/batch/markAsRead/{sessionId}接口，批量标为已读。
     * 数据一致性：推送未读消息后，无需立即更新is_read，等待前端查看后调用已读接口即可，避免推送成功但前端未接收的场景导致数据异常。
     *
     * @param sessionId
     * @param receiverId
     * @return
     */
    @PutMapping("/batch/markAsRead/{sessionId}")
    @ApiOperation("批量标记会话下所有未读消息为已读（推荐前端进入会话时调用）")
    public Result<Boolean> batchMarkMsgAsReadBySessionId(
            @PathVariable String sessionId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return chatMessageService.batchMarkMsgAsReadBySessionId(sessionId, receiverId);
    }
}