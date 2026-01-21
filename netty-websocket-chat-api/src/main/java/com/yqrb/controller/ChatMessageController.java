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
}