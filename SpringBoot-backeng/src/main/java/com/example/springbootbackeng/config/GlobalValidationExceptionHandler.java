package com.example.springbootbackeng.config;

import java.util.stream.Collectors;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.springbootbackeng.common.Result;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("；"));
        return Result.error(message.isBlank() ? "请求参数校验失败" : message);
    }

    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("；"));
        return Result.error(message.isBlank() ? "请求参数绑定失败" : message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining("；"));
        return Result.error(message.isBlank() ? "请求参数不合法" : message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return Result.error("请求体格式错误或字段类型不匹配");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage();
        return Result.error(message == null || message.isBlank() ? "非法请求" : message);
    }

    private String formatFieldError(FieldError e) {
        String field = e.getField();
        String message = e.getDefaultMessage();
        if (field == null || field.isBlank()) {
            return message == null ? "参数错误" : message;
        }
        return field + ": " + (message == null ? "参数错误" : message);
    }
}
