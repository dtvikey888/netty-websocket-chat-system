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
import java.util.List;

@RestController
@RequestMapping("/newspaper/application")
@Api(tags = "登报申请接口")
@Validated // 开启方法参数校验（如路径参数、请求参数）
public class NewspaperApplicationController {

    @Resource
    private NewspaperApplicationService newspaperApplicationService;

    @PostMapping("/submit")
    @ApiOperation("提交登报申请")
    public Result<NewspaperApplicationVO> submitApplication(
            @ApiParam(value = "登报申请信息", required = true)
            @Valid @RequestBody NewspaperApplicationVO application, // 核心：添加@Valid触发VO校验
            @ApiParam(value = "用户会话标识ReceiverId", required = true)
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return newspaperApplicationService.submitApplication(application, receiverId);
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

    @PutMapping("/audit")
    @ApiOperation("审核登报申请")
    public Result<Boolean> auditApp(
            @ApiParam(value = "申请唯一标识appId", required = true)
            @RequestParam String appId,
            @ApiParam(value = "审核状态：PENDING/AUDITED/PAID/REJECTED", required = true)
            @RequestParam String status,
            @ApiParam(value = "审核备注/驳回原因（非必填）")
            @RequestParam(required = false) String auditRemark,
            @ApiParam(value = "用户会话标识ReceiverId", required = true)
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return newspaperApplicationService.auditApplication(appId, status, auditRemark, receiverId);
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