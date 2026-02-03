package com.yqrb.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
//全局统一响应
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;

    // ====== 新增：判断是否成功的核心方法 ======
    public boolean isSuccess() {
        // 以code=200作为成功的判断标准
        return this.code != null && this.code == 200;
    }

    // 操作成功（无返回数据）
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    // 操作成功（有返回数据）
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }


    // 操作失败
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }

    // 未授权（ReceiverId无效/过期）
    public static <T> Result<T> unauthorized(String msg) {
        return new Result<>(401, msg, null);
    }

    // 参数错误
    public static <T> Result<T> paramError(String msg) {
        return new Result<>(400, msg, null);
    }
}