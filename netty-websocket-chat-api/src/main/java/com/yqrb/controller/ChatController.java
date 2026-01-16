package com.yqrb.controller;

import com.yqrb.dto.ChatMessageDTO;
import com.yqrb.dto.ResultDTO;
import com.yqrb.utils.WebSocketMemoryManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 聊天历史查询 HTTP 接口（纯内存版，无数据库操作）
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @Autowired
    private WebSocketMemoryManager webSocketMemoryManager;

    /**
     * 拉取用户聊天历史记录（纯内存查询）
     */
    @GetMapping("/history")
    public ResultDTO<List<ChatMessageDTO>> getChatHistory(
            @RequestParam String userId,
            @RequestParam(required = false) String appId) {
        try {
            // 纯内存获取聊天历史（appId 暂未过滤，可后续扩展）
            List<ChatMessageDTO> chatHistory = webSocketMemoryManager.getChatHistory(userId);
            return ResultDTO.success(chatHistory, "查询聊天历史成功（纯内存）");
        } catch (Exception e) {
            log.error("查询聊天历史失败", e);
            return ResultDTO.fail("查询聊天历史失败");
        }
    }
}