package com.lawfirm.exception;

/**
 * 参数无效异常
 */
public class InvalidParameterException extends BusinessException {

    public InvalidParameterException(String message) {
        super(400, message);
    }

    public InvalidParameterException(String parameter, String reason) {
        super(400, String.format("参数%s无效: %s", parameter, reason));
    }
}
