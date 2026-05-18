package com.example.springbootbackeng.common;

import lombok.Data;

/**
 * 全局统一响应结果
 */
@Data
public class Result<T> {
    private Integer code;       // 响应码：200成功，500失败
    private String msg;         // 响应信息
    private T data;             // 响应数据

    // 成功响应
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    // 失败响应
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMsg(msg);
        result.setData(null);
        return result;
    }
}