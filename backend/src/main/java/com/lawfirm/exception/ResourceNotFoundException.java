package com.lawfirm.exception;

/**
 * 资源不存在异常
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(404, message);
    }

    public ResourceNotFoundException(String resource, Long id) {
        super(404, String.format("%s不存在 [ID: %d]", resource, id));
    }

    public ResourceNotFoundException(String resource, String fieldName, Object fieldValue) {
        super(404, String.format("%s不存在 [%s: %s]", resource, fieldName, fieldValue));
    }
}
