package com.yqrb.pojo.vo;

/**
 * 全局响应状态码常量类
 * 统一管理所有状态码，避免硬编码，便于维护
 */
public class ResultCode {
    // 成功
    public static final Integer SUCCESS = 200;
    // 参数错误
    public static final Integer PARAM_ERROR = 400;
    // 未授权
    public static final Integer UNAUTHORIZED = 401;
    // 权限禁止
    public static final Integer FORBIDDEN = 403;
    // 资源不存在
    public static final Integer NOT_FOUND = 404;
    // 系统异常
    public static final Integer SERVER_ERROR = 500;

    // 成功默认消息
    public static final String SUCCESS_MSG = "操作成功";
    // 系统异常默认消息
    public static final String SERVER_ERROR_MSG = "系统繁忙，请稍后再试";
}