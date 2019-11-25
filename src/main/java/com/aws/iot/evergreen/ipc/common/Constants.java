package com.aws.iot.evergreen.ipc.common;

public class Constants {

    /**
     IPC Service responds to a request with a message with opcode = ERROR_OP_CODE when
        1. request had an invalid opcode
        2. call back for an opcode returned an exception to IPC service
     */
    public final static int ERROR_OP_CODE = 1;
    public final static int AUTH_OP_CODE = 2;
    public final static int PING_OP_CODE = 60;

}
