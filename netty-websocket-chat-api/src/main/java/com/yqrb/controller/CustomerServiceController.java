package com.yqrb.controller;


import com.yqrb.pojo.vo.CustomerServiceVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.CustomerServiceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/newspaper/customer")
@Api(tags = "客服信息接口")
public class CustomerServiceController {

    @Resource
    private CustomerServiceService customerServiceService;

    @GetMapping("/detail/{serviceStaffId}")
    @ApiOperation("查询客服详情")
    public Result<CustomerServiceVO> getCustomerDetail(
            @PathVariable String serviceStaffId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return customerServiceService.getCustomerByStaffId(serviceStaffId, receiverId);
    }

    @GetMapping("/list/online")
    @ApiOperation("查询所有在线客服")
    public Result<List<CustomerServiceVO>> getOnlineCustomerList(
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return customerServiceService.getOnlineCustomerList(receiverId);
    }

    @GetMapping("/list/all")
    @ApiOperation("查询所有客服")
    public Result<List<CustomerServiceVO>> getAllCustomerList(
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return customerServiceService.getAllCustomerList(receiverId);
    }

    @PutMapping("/login/{serviceStaffId}")
    @ApiOperation("客服登录（更新在线状态）")
    public Result<Boolean> customerLogin(
            @PathVariable String serviceStaffId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return customerServiceService.customerLogin(serviceStaffId, receiverId);
    }

    @PutMapping("/logout/{serviceStaffId}")
    @ApiOperation("客服登出（更新离线状态）")
    public Result<Boolean> customerLogout(
            @PathVariable String serviceStaffId,
            @RequestHeader("ReceiverId") String receiverId
    ) {
        return customerServiceService.customerLogout(serviceStaffId, receiverId);
    }
}