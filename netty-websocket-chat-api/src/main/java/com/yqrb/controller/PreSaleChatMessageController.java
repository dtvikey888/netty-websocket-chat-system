package com.yqrb.controller;

import com.yqrb.pojo.po.PreSaleChatMessagePO;
import com.yqrb.pojo.vo.PreSaleChatMessageVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.PreSaleChatMessageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 售前咨询聊天记录Controller
 */
@RestController
@RequestMapping("/api/pre-sale/chat")
@Api(tags = "售前聊天消息接口")
public class PreSaleChatMessageController {

    @Resource
    private PreSaleChatMessageService preSaleChatMessageService;

    /**
     * 售前WebSocket重连-推送该会话未读消息
     * @param sessionId 售前会话ID
     * @param receiverId 售前接收方ID（客户/售前客服）
     * @return 统一响应结果
     */
    @GetMapping("/ws/reconnect")
    @ApiOperation("售前WebSocket重连-推送该会话未读消息")
    public Result<Void> wsReconnect(
            @RequestParam String sessionId,
            @RequestParam String receiverId // 必传，限定接收方
    ) {
        return preSaleChatMessageService.wsReconnectPushUnread(sessionId, receiverId);
    }


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
     * 接口3：按会话ID查询售前未读消息（用户/售前客服查看当前会话时调用）
     * @param preSaleSessionId 售前会话ID
     * @return 消息列表
     */
    @GetMapping("/list-by-session/{preSaleSessionId}")
    @ApiOperation("按会话ID查询售前消息")
    public Result<List<PreSaleChatMessagePO>> listByPreSaleSessionId(@PathVariable String preSaleSessionId,
                                                                     @RequestHeader("ReceiverId") String receiverId) {
        return preSaleChatMessageService.listUnreadBySessionAndReceiver(preSaleSessionId,receiverId);
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

    // ====================== 新增接口1：批量标记会话未读消息为已读 ======================
    @PutMapping("/batch/markAsRead/{sessionId}")
    @ApiOperation("售前-批量标记会话下所有未读消息为已读（推荐前端进入会话时调用）")
    public Result<Boolean> batchMarkMsgAsReadBySessionId(
            @ApiParam("售前会话ID") @PathVariable String sessionId,
            @ApiParam("接收者ID（请求头传递）") @RequestHeader("ReceiverId") String receiverId
    ) {
        return preSaleChatMessageService.batchMarkMsgAsReadBySessionId(sessionId, receiverId);
    }

    // ====================== 新增接口2：查询未读消息总数（Redis缓存优化） ======================
    @GetMapping("/unread/count")
    @ApiOperation("售前-查询未读消息总数（缓存优化，高性能，适合小红点展示）")
    public Result<Long> getUnreadMsgTotalCount(
            @ApiParam("接收者ID（请求头传递）") @RequestHeader("ReceiverId") String receiverId
    ) {
        return preSaleChatMessageService.getUnreadMsgTotalCount(receiverId);
    }

    // 可选新增：分页查询售前会话消息（和售后对齐，前端滚动加载用）
    @GetMapping("/list/page/{preSaleSessionId}")
    @ApiOperation("售前-分页查询会话消息列表（推荐前端滚动加载/分页展示时调用）")
    public Result<List<PreSaleChatMessageVO>> getMessageListWithPage(
            @ApiParam("售前会话ID") @PathVariable String preSaleSessionId,
            @ApiParam("接收者ID（请求头传递）") @RequestHeader("ReceiverId") String receiverId,
            @ApiParam("页码（默认第1页）") @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @ApiParam("每页条数（默认10条，最大100条）") @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        return preSaleChatMessageService.listByPreSaleSessionIdWithPage(preSaleSessionId, receiverId, pageNum, pageSize);
    }

    // 可选新增：按会话ID删除售前所有消息（和售后对齐）
    @DeleteMapping("/delete/session/{sessionId}")
    @ApiOperation("售前-删除会话所有消息（按sessionId）")
    public Result<Boolean> deleteMessageBySessionId(
            @PathVariable String sessionId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return preSaleChatMessageService.deleteMessageBySessionId(sessionId, receiverId);
    }
}