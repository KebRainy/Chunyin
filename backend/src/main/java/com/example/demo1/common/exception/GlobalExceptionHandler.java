package com.example.demo1.common.exception;

import com.example.demo1.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("Business exception: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleBadCredentialsException(BadCredentialsException e) {
        log.error("Authentication failed: {}", e.getMessage());
        return Result.error(401, "用户名或密码错误");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleAccessDeniedException(AccessDeniedException e) {
        log.error("Access denied: {}", e.getMessage());
        return Result.error(403, "权限不足");
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidationException(Exception e) {
        String message = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException ex) {
            if (ex.getBindingResult().hasErrors()) {
                message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
            }
        } else if (e instanceof BindException ex) {
            if (ex.getBindingResult().hasErrors()) {
                message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
            }
        }
        log.error("Validation failed: {}", message);
        return Result.error(400, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMissingParameterException(MissingServletRequestParameterException e) {
        String parameterName = e.getParameterName();
        String message = String.format("缺少必需参数: %s", parameterName);
        log.error("Missing required parameter: {}", parameterName);
        return Result.error(400, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("System exception: ", e);
        // 记录详细的错误信息到日志
        String errorMessage = e.getMessage();
        String className = e.getClass().getName();
        
        // 记录异常堆栈
        log.error("Exception type: {}", className);
        if (e.getCause() != null) {
            log.error("Exception cause: {}", e.getCause().getMessage());
            log.error("Cause stack trace: ", e.getCause());
        }
        
        // 对于数据库相关异常，返回更友好的错误信息
        if (errorMessage != null) {
            if (errorMessage.contains("foreign key")) {
                return Result.error("数据关联错误，请检查相关数据是否存在");
            }
            if (errorMessage.contains("Cannot add or update")) {
                return Result.error("数据保存失败，请检查数据格式是否正确");
            }
            if (errorMessage.contains("Unknown column") || errorMessage.contains("doesn't exist")) {
                log.error("Database column does not exist, please check database structure");
                return Result.error("数据库结构错误，请联系管理员检查数据库");
            }
            if (errorMessage.contains("JSON") || errorMessage.contains("json")) {
                log.error("JSON serialization/deserialization error");
                return Result.error("数据格式错误，请检查输入数据");
            }
        }
        
        // 记录完整的错误信息
        log.error("Full error message: {}", errorMessage);
        return Result.error("系统异常，请联系管理员。错误类型: " + className);
    }
}
