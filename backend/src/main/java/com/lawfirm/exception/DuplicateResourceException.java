package com.lawfirm.exception;

/**
 * 资源重复异常
 */
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super(409, message);
    }

    public DuplicateResourceException(String resource, String fieldName, Object fieldValue) {
        super(409, String.format("%s已存在 [%s: %s]", resource, fieldName, fieldValue));
    }
}
