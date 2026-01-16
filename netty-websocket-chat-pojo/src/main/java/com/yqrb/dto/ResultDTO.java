package com.yqrb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * HTTP 接口统一返回结果（前后端分离数据格式统一）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultDTO<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 响应码（200成功，500失败） */
    private Integer code;

    /** 响应消息 */
    private String msg;

    /** 响应数据 */
    private T data;

    // 成功响应（无数据）
    public static <T> ResultDTO<T> success(String msg) {
        return new ResultDTO<>(200, msg, null);
    }

    // 成功响应（带数据）
    public static <T> ResultDTO<T> success(T data, String msg) {
        return new ResultDTO<>(200, msg, data);
    }

    // 失败响应
    public static <T> ResultDTO<T> fail(String msg) {
        return new ResultDTO<>(500, msg, null);
    }
}