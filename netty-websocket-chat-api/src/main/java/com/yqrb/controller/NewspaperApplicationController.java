package com.yqrb.controller;

import com.yqrb.pojo.vo.NewspaperApplicationVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.NewspaperApplicationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/newspaper/application")
@Api(tags = "登报申请接口")
public class NewspaperApplicationController {

    @Resource
    private NewspaperApplicationService newspaperApplicationService;

    @PostMapping("/submit")
    @ApiOperation("提交登报申请")
    public Result<NewspaperApplicationVO> submitApplication(
            @RequestBody NewspaperApplicationVO application,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return newspaperApplicationService.submitApplication(application, receiverId);
    }

    @GetMapping("/detail/{appId}")
    @ApiOperation("查询申请详情")
    public Result<NewspaperApplicationVO> getAppDetail(
            @PathVariable String appId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return newspaperApplicationService.getApplicationByAppId(appId, receiverId);
    }

    @GetMapping("/list/user/{userId}")
    @ApiOperation("查询用户申请列表")
    public Result<List<NewspaperApplicationVO>> getAppListByUser(
            @PathVariable String userId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return newspaperApplicationService.getApplicationListByUserId(userId, receiverId);
    }

    @GetMapping("/list/cs/{serviceStaffId}")
    @ApiOperation("查询客服处理申请列表")
    public Result<List<NewspaperApplicationVO>> getAppListByCs(
            @PathVariable String serviceStaffId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return newspaperApplicationService.getApplicationListByServiceStaffId(serviceStaffId, receiverId);
    }

    @PutMapping("/audit")
    @ApiOperation("审核登报申请")
    public Result<Boolean> auditApp(
            @RequestParam String appId,
            @RequestParam String status,
            @RequestParam(required = false) String auditRemark,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return newspaperApplicationService.auditApplication(appId, status, auditRemark, receiverId);
    }

    @DeleteMapping("/delete/{appId}")
    @ApiOperation("删除登报申请")
    public Result<Boolean> deleteApp(
            @PathVariable String appId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return newspaperApplicationService.deleteApplication(appId, receiverId);
    }
}