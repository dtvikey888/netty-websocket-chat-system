package com.yqrb.controller;

import com.yqrb.pojo.vo.Result;
import com.yqrb.pojo.vo.SessionMappingVO;
import com.yqrb.service.SessionMappingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/yqrb/session")
@Api(tags = "会话映射接口")
public class SessionMappingController {

    @Resource
    private SessionMappingService sessionMappingService;

    // ========== 原有接口 ==========
    @PostMapping("/create")
    @ApiOperation("创建会话映射")
    public Result<SessionMappingVO> createSessionMapping(
            @RequestBody SessionMappingVO sessionMapping,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return sessionMappingService.createSessionMapping(sessionMapping, receiverId);
    }

    @GetMapping("/session/{sessionId}")
    @ApiOperation("按会话ID查询映射")
    public Result<SessionMappingVO> getBySessionId(
            @PathVariable String sessionId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return sessionMappingService.getBySessionId(sessionId, receiverId);
    }

    @GetMapping("/app/{appId}")
    @ApiOperation("按申请ID查询映射")
    public Result<SessionMappingVO> getByAppId(
            @PathVariable String appId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return sessionMappingService.getByAppId(appId, receiverId);
    }

    @GetMapping("/user/{userId}")
    @ApiOperation("按用户ID查询会话列表")
    public Result<List<SessionMappingVO>> getByUserId(
            @PathVariable String userId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return sessionMappingService.getByUserId(userId, receiverId);
    }

    @DeleteMapping("/delete/{sessionId}")
    @ApiOperation("按会话ID删除映射")
    public Result<Boolean> deleteBySessionId(
            @PathVariable String sessionId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return sessionMappingService.deleteBySessionId(sessionId, receiverId);
    }

    // ========== 新增接口 ==========
    @GetMapping("/service/{serviceStaffId}")
    @ApiOperation("按客服ID查询承接的会话列表")
    public Result<List<SessionMappingVO>> getByServiceStaffId(
            @PathVariable String serviceStaffId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return sessionMappingService.getByServiceStaffId(serviceStaffId, receiverId);
    }

    @GetMapping("/user-app")
    @ApiOperation("按用户+申请ID查询会话")
    public Result<SessionMappingVO> getByUserIdAndAppId(
            @RequestParam String userId,
            @RequestParam String appId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return sessionMappingService.getByUserIdAndAppId(userId, appId, receiverId);
    }

    @PutMapping("/update")
    @ApiOperation("更新会话映射（如更换客服）")
    public Result<Boolean> updateSessionMapping(
            @RequestBody SessionMappingVO sessionMapping,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return sessionMappingService.updateSessionMapping(sessionMapping, receiverId);
    }

    @DeleteMapping("/delete/app/{appId}")
    @ApiOperation("按申请ID删除会话映射")
    public Result<Boolean> deleteByAppId(
            @PathVariable String appId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return sessionMappingService.deleteByAppId(appId, receiverId);
    }
}