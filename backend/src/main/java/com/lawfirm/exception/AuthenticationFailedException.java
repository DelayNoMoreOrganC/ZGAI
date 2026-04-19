package com.lawfirm.exception;

/**
 * 认证失败异常
 */
public class AuthenticationFailedException extends BusinessException {

    public AuthenticationFailedException(String message) {
        super(401, message);
    }

    public AuthenticationFailedException() {
        super(401, "认证失败");
    }
}
