package com.yqrb.controller;

import com.yqrb.pojo.vo.NewspaperApplicationVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.NewspaperApplicationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/newspaper/application")
@Api(tags = "登报申请接口")
@Validated // 开启方法参数校验（如路径参数、请求参数）
public class NewspaperApplicationController {

    @Resource
    private NewspaperApplicationService newspaperApplicationService;

    /**
     * 我是客服 那么 LYQY_CS_5fbb6357b77d2e6436a46336
     * 我也是普通用户 那么  LYQY_USER_5fbb6357b77d2e6436a46336
     * 前端我打开会有两个按钮 一个选普通用户进去是普通用户申请页面 前缀 LYQY_USER_
     * 选了客服那么就是进去客服页面 前缀 LYQY_CS_
     *
     * 你理解的完全正确！后端通过 WebSocket 推送「新申请提醒」只是第一步，前端客服页面确实还需要基于 WebSocket 实现「申请数据的实时更新」—— 但这里要区分「轻量提醒推送」和「完整数据更新」的逻辑，不需要做复杂的全量数据推送，有两种更优的实现方案，既满足「实时更新」需求，又能避免服务端压力过大。
     * 一、 先理清两个核心概念（避免混淆）
     * 目前你已实现的：「新申请提醒推送」（通知型，轻量）
     * 内容：仅包含 appId、申请人、联系电话、申请类型等关键简化信息。
     * 作用：告知客服「有新申请来了」，起到「即时提醒」的作用（比如前端弹出消息提示、播放提示音）。
     * 局限性：只是「通知」，无法直接更新「待处理申请列表」的完整数据（比如列表的分页、筛选、其他扩展字段）。
     * 需要补充的：「申请数据实时更新」（数据型，完整）
     * 内容：完整的待处理申请列表 / 单条申请详情数据。
     * 作用：让客服页面的「待处理申请列表」无需手动刷新，自动同步最新数据。
     * 实现依赖：依然基于 WebSocket，但无需后端额外复杂开发，有两种高效方案可选（优先推荐第一种）。
     * 二、 两种最优实现方案（兼顾简单性和可靠性）
     * 方案一：「提醒推送 + 前端收到提醒后主动拉取完整数据」（推荐优先实现）
     * 这是生产环境最常用、最可靠的方案，兼顾「低耦合、低压力、高可用」，核心逻辑是：后端只负责推「轻量提醒」，前端收到提醒后，调用已有的 /list/cs/{serviceStaffId} 接口拉取完整待处理列表。
     * 具体流程
     * 后端（保持现有逻辑，无需额外修改）
     * 完成登报申请入库后，继续向对应客服推送「轻量新申请提醒」，消息格式中保留 appId 作为核心标识，示例：
     * json
     * {
     *   "msgType": "SYSTEM_NEW_APPLICATION", // 消息类型，用于前端区分
     *   "content": "【新登报申请提醒】\n申请ID：APP_1c79ac7b047740a5a67b99e6f3a3906b\n申请人：乐音清扬普通用户\n联系电话：13800138000\n申请类型：遗失声明\n提交时间：2026-01-29 18:06:39",
     *   "appId": "APP_1c79ac7b047740a5a67b99e6f3a3906b",
     *   "receiverId": "LYQY_CS_5fbb6357b77d2e6436a46336"
     * }
     * 前端（核心改造，实现「事件驱动式刷新」）
     * 第一步：在客服页面初始化时，正常调用 /list/cs/{serviceStaffId} 接口，加载初始待处理申请列表。
     * 第二步：建立 WebSocket 连接，监听 msgType = "SYSTEM_NEW_APPLICATION" 的消息（新申请提醒）。
     * 第三步：当收到该提醒消息时，触发「列表刷新逻辑」（重新调用 /list/cs/{serviceStaffId} 接口），获取最新的待处理列表，替换页面原有数据，实现「实时更新」。
     * 额外优化：前端可添加「消息提示音」「列表第一条高亮」，提升客服感知度。
     * 方案优势（为什么推荐）
     * 简单易实现：复用现有 /list/cs/{serviceStaffId} 接口，无需后端额外开发新接口，前端改造成本极低。
     * 低耦合：后端不关心前端的列表展示逻辑（分页、筛选、排序），只负责推送「有新数据」的通知，前端自主控制数据拉取，后续列表逻辑变更无需改动后端。
     * 高可靠：即使 WebSocket 消息偶尔丢失，客服手动刷新页面也能获取完整数据，不会出现「数据不一致」的问题；同时避免了「后端推送完整列表」的传输压力。
     * 大幅降低服务端压力：从「客服定时轮询（比如每 30 秒一次）」变为「有新申请才触发拉取」，查询请求量会大幅减少（比如从「每个客服每分钟 2 次查询」变为「每天几十次查询」），完全达到你的核心价值目标。
     * 方案优势（为什么推荐）
     * 简单易实现：复用现有 /list/cs/{serviceStaffId} 接口，无需后端额外开发新接口，前端改造成本极低。
     * 低耦合：后端不关心前端的列表展示逻辑（分页、筛选、排序），只负责推送「有新数据」的通知，前端自主控制数据拉取，后续列表逻辑变更无需改动后端。
     * 高可靠：即使 WebSocket 消息偶尔丢失，客服手动刷新页面也能获取完整数据，不会出现「数据不一致」的问题；同时避免了「后端推送完整列表」的传输压力。
     * 大幅降低服务端压力：从「客服定时轮询（比如每 30 秒一次）」变为「有新申请才触发拉取」，查询请求量会大幅减少（比如从「每个客服每分钟 2 次查询」变为「每天几十次查询」），完全达到你的核心价值目标。
     *
     * 用户进入登报系统/登录成功/进入申请页面 → 调用「生成 receiverId 接口」→ 后端生成 receiverId + 存入 Redis + 返回给前端
     *     ↓（前端存储 receiverId，后续所有请求都携带它）
     * 用户填写申请信息，点击「提交」→ 前端将 receiverId 放在 Header 中，和申请数据一起提交给后端
     *     ↓
     * 后端拿到 Header 中的 receiverId → 做合法性校验（查 Redis）→ 校验通过则处理申请，不通过则直接拦截
     *
     * 用户提交登报申请 → 申请入库+会话映射入库 → 提取客服ID
     *     ↓
     * 判断客服是否在线？
     *     → 是（有WebSocket通道）：尝试实时推送WebSocket提醒
     *         → 推送成功：打印成功日志
     *         → 推送失败：触发降级，存储离线消息
     *     → 否（无WebSocket通道）：直接存储离线消息
     *     ↓
     * 刷新ReceiverId过期时间 → 返回申请详情给用户
     *
     * 提交登报申请（带幂等性校验，支持5分钟内提交不同申请）
     * @param application 登报申请信息
     * @param receiverId 用户会话标识
     * @param requestId 幂等请求标识（前端生成UUID，每笔新申请对应一个新ID）
     * @return
     */
    @PostMapping("/submit")
    @ApiOperation("提交登报申请")
    public Result<NewspaperApplicationVO> submitApplication(
            @ApiParam(value = "登报申请信息", required = true)
            @Valid @RequestBody NewspaperApplicationVO application,
            @ApiParam(value = "用户会话标识ReceiverId", required = true)
            @RequestHeader("ReceiverId") String receiverId,
            @ApiParam(value = "幂等请求标识（前端生成UUID，每笔新申请需生成新ID）", required = true)
            @RequestHeader("Request-Id") String requestId // 每笔不同申请对应不同requestId，正常放行
    ) {
        // 透传requestId到服务层做幂等校验
        return newspaperApplicationService.submitApplication(application, receiverId, requestId);
    }

    // 全局异常处理器：捕获参数校验异常，返回友好的400错误
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)  // 返回400状态码（而非500）
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder errorMsg = new StringBuilder();
        // 拼接所有校验失败的字段和提示
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errorMsg.append(fieldError.getField()).append(":").append(fieldError.getDefaultMessage()).append(";");
        }
        // 返回参数错误提示
        return Result.paramError(errorMsg.toString());
    }

    @GetMapping("/detail/{appId}")
    @ApiOperation("查询申请详情")
    public Result<NewspaperApplicationVO> getAppDetail(
            @ApiParam(value = "申请唯一标识appId", required = true)
            @PathVariable String appId,
            @ApiParam(value = "用户会话标识ReceiverId", required = true)
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return newspaperApplicationService.getApplicationByAppId(appId, receiverId);
    }

    @GetMapping("/list/user/{userId}")
    @ApiOperation("查询用户申请列表")
    public Result<List<NewspaperApplicationVO>> getAppListByUser(
            @ApiParam(value = "用户ID", required = true)
            @PathVariable String userId,
            @ApiParam(value = "用户会话标识ReceiverId", required = true)
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return newspaperApplicationService.getApplicationListByUserId(userId, receiverId);
    }

    /**
     * 该接口是客服专用接口，供客服查询自己负责处理的所有登报申请列表。
     * @param serviceStaffId
     * @param receiverId
     * @return
     */
    @GetMapping("/list/cs/{serviceStaffId}")
    @ApiOperation("查询客服处理申请列表")
    public Result<List<NewspaperApplicationVO>> getAppListByCs(
            @ApiParam(value = "客服ID", required = true)
            @PathVariable String serviceStaffId,
            @ApiParam(value = "用户会话标识ReceiverId", required = true)
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return newspaperApplicationService.getApplicationListByServiceStaffId(serviceStaffId, receiverId);
    }

    /**
     * 这个接口是「客服专用接口」，用于客服在工作台对普通用户提交的登报申请进行「审核处理」，
     * 核心功能包括「审核通过（设置付款金额）」「审核驳回（填写驳回原因）」「标记已支付」，同时会自动给用户推送对应的通知消息。
     * 一、接口核心定位与使用场景
     * 调用方：仅客服工作台前端（普通用户无此操作权限）。
     * 使用场景：客服在工作台看到新申请后，查看申请详情，进行审核操作（通过 / 驳回）；用户完成付款后，客服确认款项到账，标记申请为「已支付」。
     * 核心价值：承接登报申请的「审核流程」，是连接「用户提交申请」和「用户完成付款」的关键环节，同时通过自动推送消息，让用户实时知晓审核结果。
     * @param appId
     * @param status
     * @param auditRemark
     * @param payAmount
     * @param receiverId
     * @return
     */
    @PutMapping("/audit")
    @ApiOperation("审核登报申请（审核人手动设置付款金额）")
    public Result<Boolean> auditApp(
            @ApiParam(value = "申请唯一标识appId", required = true) @RequestParam String appId,
            @ApiParam(value = "审核状态：PENDING/AUDITED/PAID/REJECTED", required = true) @RequestParam String status,
            @ApiParam(value = "审核备注/驳回原因（非必填）") @RequestParam(required = false) String auditRemark,
            // 新增：付款金额（仅审核通过时必填）
            @ApiParam(value = "付款金额（审核通过时必填，大于0）") @RequestParam(required = false) BigDecimal payAmount,
            @ApiParam(value = "用户会话标识ReceiverId", required = true) @RequestHeader("ReceiverId") String receiverId
    ) {
        // 透传payAmount参数给服务层
        return newspaperApplicationService.auditApplication(appId, status, auditRemark, payAmount, receiverId);
    }
    @DeleteMapping("/delete/{appId}")
    @ApiOperation("删除登报申请")
    public Result<Boolean> deleteApp(
            @ApiParam(value = "申请唯一标识appId", required = true)
            @PathVariable String appId,
            @ApiParam(value = "用户会话标识ReceiverId", required = true)
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return newspaperApplicationService.deleteApplication(appId, receiverId);
    }
}