package com.aws.iot.evergreen.ipc.services.shadow.exception;

public class ShadowIPCException extends Exception {
    private static final long serialVersionUID = 1521863287768384490L;

    public ShadowIPCException(Throwable e) {
        super(e);
    }

    public ShadowIPCException(String errorMessage) {
        super(errorMessage);
    }
}
