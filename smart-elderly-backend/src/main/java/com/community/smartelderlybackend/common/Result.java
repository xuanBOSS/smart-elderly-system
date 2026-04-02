package com.community.smartelderlybackend.common;

import lombok.Data;

/**
 * 后端统一返回结果集（万能包装盒）
 */
@Data
public class Result<T> {
    private Integer code; // 状态码：200代表成功，500代表失败
    private String message; // 提示信息
    private T data; // 真正要返回的数据

    // 成功时的快速返回方法（带数据）
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    // 成功时的快速返回方法（不带数据，只提示成功）
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    // 失败时的快速返回方法
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }
}