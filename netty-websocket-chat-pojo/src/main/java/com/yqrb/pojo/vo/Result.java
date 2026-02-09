package com.yqrb.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全局统一返回结果类（通用，售前售后共用）
 * 泛型T支持任意数据类型的返回，覆盖单对象、集合、分页等所有场景
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    // 响应状态码
    private Integer code;
    // 响应提示消息
    private String msg;
    // 响应业务数据
    private T data;

    // ===== 核心：判断是否成功（null安全+正确的数值比较）=====
    public boolean isSuccess() {
        // 1. 先判空避免NPE  2. 用equals()比较Integer数值（而非==比较引用）
        return this.code != null && this.code.equals(ResultCode.SUCCESS);
    }

    // ===================== 基础成功响应 =====================
    // 成功：无数据、默认消息
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS, ResultCode.SUCCESS_MSG, null);
    }

    // 成功：有数据、默认消息
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS, ResultCode.SUCCESS_MSG, data);
    }

    // 成功：有数据、自定义成功消息（更友好，如“查询成功”“新增成功”）
    public static <T> Result<T> success(T data, String msg) {
        // 简化冗余的三元表达式 → 直接使用data
        return new Result<>(ResultCode.SUCCESS, msg, data);
    }

    // ===================== 基础失败响应 =====================
    // 失败：500系统异常、自定义消息
    public static <T> Result<T> error(String msg) {
        return new Result<>(ResultCode.SERVER_ERROR, msg == null ? ResultCode.SERVER_ERROR_MSG : msg, null);
    }

    // 未授权：401（token无效/过期、ReceiverId无效等）
    public static <T> Result<T> unauthorized(String msg) {
        return new Result<>(ResultCode.UNAUTHORIZED, msg, null);
    }

    // 参数错误：400（入参为空、格式错误、校验失败等）
    public static <T> Result<T> paramError(String msg) {
        return new Result<>(ResultCode.PARAM_ERROR, msg, null);
    }

    // 资源不存在：404（查询的对象/数据不存在，高频业务场景）
    public static <T> Result<T> notFound(String msg) {
        return new Result<>(ResultCode.NOT_FOUND, msg, null);
    }

    // 权限禁止：403（已授权，但无操作该资源的权限）
    public static <T> Result<T> forbidden(String msg) {
        return new Result<>(ResultCode.FORBIDDEN, msg, null);
    }

    // ===================== 自定义响应（适配特殊业务）=====================
    // 自定义状态码+消息+无数据（适用于上述未覆盖的特殊状态码）
    public static <T> Result<T> custom(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    // 自定义状态码+消息+数据（极致灵活，覆盖所有特殊业务场景）
    public static <T> Result<T> custom(Integer code, String msg, T data) {
        return new Result<>(code, msg, data);
    }
}