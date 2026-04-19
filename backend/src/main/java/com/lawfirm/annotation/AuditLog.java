package com.lawfirm.annotation;

import java.lang.annotation.*;

/**
 * 操作审计日志注解
 * 用于标记需要记录审计日志的方法
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    /**
     * 操作描述
     */
    String value() default "";

    /**
     * 操作类型
     */
    String operationType() default "OTHER";

    /**
     * 是否记录请求参数
     */
    boolean logParams() default true;

    /**
     * 是否记录响应结果
     */
    boolean logResult() default false;
}