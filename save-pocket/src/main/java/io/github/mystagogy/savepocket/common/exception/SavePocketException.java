package io.github.mystagogy.savepocket.common.exception;

public class SavePocketException extends RuntimeException {
    private final ErrorCode errorCode;

    public SavePocketException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public SavePocketException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
