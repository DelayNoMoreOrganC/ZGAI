package com.lawfirm.exception;

/**
 * 权限不足异常
 */
public class PermissionDeniedException extends BusinessException {

    public PermissionDeniedException(String message) {
        super(403, message);
    }

    public PermissionDeniedException() {
        super(403, "权限不足，禁止访问");
    }
}
