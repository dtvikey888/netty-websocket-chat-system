package com.yqrb.controller;

import com.yqrb.pojo.vo.ReceiverIdSessionVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.ReceiverIdService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * ReceiverId 会话管理控制器
 * 提供通用ReceiverId生成和乐音清扬用户专属固定ReceiverId生成接口
 */
@RestController
@RequestMapping("/receiver")
@Api(tags = "ReceiverId管理接口")
public class ReceiverIdController {

    @Resource
    private ReceiverIdService receiverIdService;

    // 定义合法的用户类型集合，便于后续维护和扩展
    private static final List<String> VALID_USER_TYPES = Arrays.asList("USER", "ADMIN", "CS");

    /**
     * 生成ReceiverId（通用版）
     * 适配非乐音清扬用户，生成随机ReceiverId
     *
     * @param userId   非乐音清扬用户ID（不能为空）
     * @param userName 用户名（不能为空）
     * @return 包含ReceiverId会话信息的返回结果
     */
//    @PostMapping("/generate")
//    @ApiOperation("生成ReceiverId（通用版）")
//    public Result<ReceiverIdSessionVO> generateReceiverId(
//            @ApiParam("用户ID（非乐音清扬用户使用，不可为空）") @RequestParam String userId,
//            @ApiParam("用户名（不可为空）") @RequestParam String userName
//    ) {
//        // 补充通用接口的参数非空校验，避免空指针异常
//        if (!StringUtils.hasText(userId)) {
//            return Result.paramError("用户ID不能为空");
//        }
//        if (!StringUtils.hasText(userName)) {
//            return Result.paramError("用户名不能为空");
//        }
//
//        ReceiverIdSessionVO session = receiverIdService.generateReceiverId(userId, userName);
//        return Result.success(session);
//    }

    /**
     * 生成乐音清扬用户固定ReceiverId（按appId）
     * 适配乐音清扬用户，生成格式固定的ReceiverId，便于测试和兼容
     *
     * @param userType 用户类型：USER/ADMIN/CS（仅支持这三种）
     * @param appId    用户专属appId（如001、abc123，不可为空）
     * @param userName 用户名（如：乐音清扬普通用户，不可为空）
     * @return 包含固定ReceiverId会话信息的返回结果
     */
    @PostMapping("/generate/lyqy")
    @ApiOperation("生成乐音清扬用户固定ReceiverId（按appId）")
    public Result<ReceiverIdSessionVO> generateLyqyFixedReceiverId(
            @ApiParam("用户类型：USER/ADMIN/CS") @RequestParam String userType,
            @ApiParam("用户专属appId（如001、abc123，不可为空）") @RequestParam String appId,
            @ApiParam("用户名（如：乐音清扬普通用户，不可为空）") @RequestParam String userName
    ) {
        // 1. 校验参数合法性，优化用户类型判断逻辑，更简洁易扩展
        if (!VALID_USER_TYPES.contains(userType)) {
            return Result.paramError("用户类型仅支持USER/ADMIN/CS");
        }
        if (!StringUtils.hasText(appId)) {
            return Result.paramError("用户专属appId不能为空");
        }
        if (!StringUtils.hasText(userName)) {
            return Result.paramError("用户名不能为空");
        }

        // 2. 生成乐音清扬用户ID（格式：LYQY_<userType>_<appId>），去除appId前后空格，提升健壮性
        String lyqyUserId = String.format("LYQY_%s_%s", userType, appId.trim());

        // 3. 调用服务层生成固定ReceiverId（已在ReceiverIdServiceImpl中适配）
        ReceiverIdSessionVO session = receiverIdService.generateReceiverId(lyqyUserId, userName);
        return Result.success(session);
    }
}