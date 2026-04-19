package com.lawfirm.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.util.Result;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理所有异常，确保API返回格式一致
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理业务异常 - 基础类型
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.error("业务异常: URI={}, Code={}, Message={}", request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理资源不存在异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        log.error("资源不存在: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return Result.notFound(e.getMessage());
    }

    /**
     * 处理权限不足异常
     */
    @ExceptionHandler(PermissionDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handlePermissionDeniedException(PermissionDeniedException e, HttpServletRequest request) {
        log.error("权限不足: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return Result.forbidden(e.getMessage());
    }

    /**
     * 处理资源重复异常
     */
    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Void> handleDuplicateResourceException(DuplicateResourceException e, HttpServletRequest request) {
        log.error("资源重复: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return Result.error(409, e.getMessage());
    }

    /**
     * 处理参数无效异常
     */
    @ExceptionHandler(InvalidParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleInvalidParameterException(InvalidParameterException e, HttpServletRequest request) {
        log.error("参数无效: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return Result.validationError(e.getMessage());
    }

    /**
     * 处理认证失败异常
     */
    @ExceptionHandler(AuthenticationFailedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationFailedException(AuthenticationFailedException e, HttpServletRequest request) {
        log.error("认证失败: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return Result.unauthorized(e.getMessage());
    }

    /**
     * 处理参数校验异常（RequestBody）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.error("参数校验异常: URI={}, Message={}", request.getRequestURI(), errorMessage);
        return Result.validationError("参数校验失败: " + errorMessage);
    }

    /**
     * 处理参数校验异常（RequestParam/@PathVariable）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.error("参数校验异常: URI={}, Message={}", request.getRequestURI(), errorMessage);
        return Result.validationError("参数校验失败: " + errorMessage);
    }

    /**
     * 处理权限异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.error("权限异常: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return Result.forbidden("权限不足，禁止访问");
    }

    /**
     * 处理认证异常
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleBadCredentialsException(BadCredentialsException e, HttpServletRequest request) {
        log.error("认证异常: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return Result.unauthorized("用户名或密码错误");
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.error("非法参数异常: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return Result.validationError(e.getMessage());
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingRequestValueException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingRequestValueException(MissingRequestValueException e, HttpServletRequest request) {
        log.error("缺少请求参数: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return Result.validationError("缺少必需的请求参数");
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.error("参数类型不匹配: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return Result.validationError(String.format("参数 %s 的值 '%s' 类型不正确，期望类型: %s",
                e.getName(), e.getValue(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知"));
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        log.error("接口不存在: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return Result.notFound("接口不存在: " + request.getRequestURI());
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常: URI={}, Message={}", request.getRequestURI(), e.getMessage(), e);
        return Result.error("系统内部错误");
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("运行时异常: URI={}, Message={}", request.getRequestURI(), e.getMessage(), e);
        return Result.error("系统内部错误: " + e.getMessage());
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("未知异常: URI={}, Message={}", request.getRequestURI(), e.getMessage(), e);
        return Result.error("系统内部错误，请联系管理员");
    }
}
