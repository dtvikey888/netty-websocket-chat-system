package com.yqrb.controller;

import com.yqrb.pojo.vo.PreSaleChatMessageVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.PreSaleChatMessageService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
/**
 * 售前咨询聊天记录Controller
 */
@RestController
@RequestMapping("/api/pre-sale/chat")
public class PreSaleChatMessageController {

    @Resource
    private PreSaleChatMessageService preSaleChatMessageService;

    /**
     * 接口1：生成售前会话ID（用户打开售前咨询入口时调用）
     * @return 售前会话ID
     */
    @GetMapping("/generate-session-id")
    @ApiOperation("生成售前会话ID")
    public Result<String> generatePreSaleSessionId() {
        String sessionId = preSaleChatMessageService.generatePreSaleSessionId();
        return Result.success(sessionId);
    }

    /**
     * 接口2：保存售前消息（用户/客服发送消息时调用）
     * @param vo 售前消息VO
     * @return 保存结果
     */
    @PostMapping("/save")
    @ApiOperation("保存售前消息")
    public Result<Void> savePreSaleChatMessage(@RequestBody PreSaleChatMessageVO vo,
                                               @RequestHeader("ReceiverId") String receiverId) {
        return preSaleChatMessageService.savePreSaleChatMessage(vo,receiverId);
    }

    /**
     * 接口3：按会话ID查询售前消息（用户/售前客服查看当前会话时调用）
     * @param preSaleSessionId 售前会话ID
     * @return 消息列表
     */
    @GetMapping("/list-by-session/{preSaleSessionId}")
    @ApiOperation("按会话ID查询售前消息")
    public Result<List<PreSaleChatMessageVO>> listByPreSaleSessionId(@PathVariable String preSaleSessionId,
                                                                     @RequestHeader("ReceiverId") String receiverId) {
        return preSaleChatMessageService.listByPreSaleSessionId(preSaleSessionId,receiverId);
    }

    /**
     * 接口4：按用户ID查询售前消息（售后客服追溯用户售前记录时调用）
     * @param userId 用户ID
     * @return 消息列表
     */
    @GetMapping("/list-by-user/{userId}")
    @ApiOperation("按用户ID查询售前消息")
    public Result<List<PreSaleChatMessageVO>> listByUserId(@PathVariable String userId,
                                                           @RequestHeader("ReceiverId") String receiverId) {
        return preSaleChatMessageService.listByUserId(userId,receiverId);
    }
}