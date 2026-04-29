package com.example.literaturesearchsystem.common;

import lombok.Data;

@Data
public class Result<T> {

    private Integer code;
    private String message;
    private T data;
    private Long total;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    private Result(Integer code, String message, T data, Long total) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.total = total;
    }

    // 普通成功返回（只有数据）
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    // 带消息的成功返回（只有数据）
    public static <T> Result<T> successWithMsg(String message, T data) {
        return new Result<>(200, message, data);
    }

    // 带分页的成功返回
    public static <T> Result<T> successWithPage(T data, Long total) {
        return new Result<>(200, "success", data, total);
    }

    // 错误返回
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> unauthorized() {
        return new Result<>(401, "请先登录", null);
    }

    public static <T> Result<T> forbidden() {
        return new Result<>(403, "权限不足", null);
    }
}