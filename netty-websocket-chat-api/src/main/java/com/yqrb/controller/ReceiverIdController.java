package com.yqrb.controller;

import com.yqrb.pojo.vo.ReceiverIdSessionVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.ReceiverIdService;
import com.yqrb.util.UUIDUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/receiver")
@Api(tags = "ReceiverId管理接口")
public class ReceiverIdController {

    @Resource
    private ReceiverIdService receiverIdService;

    // 原有接口（保留，兼容非乐音清扬用户）
    @PostMapping("/generate")
    @ApiOperation("生成ReceiverId（通用版）")
    public Result<ReceiverIdSessionVO> generateReceiverId(
            @ApiParam("用户ID（非乐音清扬用户使用）") @RequestParam String userId,
            @ApiParam("用户名") @RequestParam String userName
    ) {
        ReceiverIdSessionVO session = receiverIdService.generateReceiverId(userId, userName);
        return Result.success(session);
    }

    // 新增：乐音清扬用户专属接口（生成固定ReceiverId）
    @PostMapping("/generate/lyqy")
    @ApiOperation("生成乐音清扬用户固定ReceiverId（按appId）")
    public Result<ReceiverIdSessionVO> generateLyqyFixedReceiverId(
            @ApiParam("用户类型：USER/ADMIN/CS") @RequestParam String userType,
            @ApiParam("用户专属appId（如001、abc123）") @RequestParam String appId,
            @ApiParam("用户名（如：乐音清扬普通用户）") @RequestParam String userName
    ) {
        // 1. 校验参数合法性
        if (!"USER".equals(userType) && !"ADMIN".equals(userType) && !"CS".equals(userType)) {
            return Result.paramError("用户类型仅支持USER/ADMIN/CS");
        }
        if (appId == null || appId.trim().isEmpty()) {
            return Result.paramError("用户专属appId不能为空");
        }

        // 2. 生成乐音清扬用户ID（格式：LYQY_<userType>_<appId>）
        String lyqyUserId = "LYQY_" + userType + "_" + appId.trim();

        // 3. 调用服务层生成固定ReceiverId（已在ReceiverIdServiceImpl中适配）
        ReceiverIdSessionVO session = receiverIdService.generateReceiverId(lyqyUserId, userName);
        return Result.success(session);
    }
}