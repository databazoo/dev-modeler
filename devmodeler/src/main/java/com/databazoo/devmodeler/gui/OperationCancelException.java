package com.databazoo.devmodeler.gui;

public class OperationCancelException extends Exception {
    public OperationCancelException() {
    }

    public OperationCancelException(String message) {
        super(message);
    }

    public OperationCancelException(String message, Throwable cause) {
        super(message, cause);
    }

    public OperationCancelException(Throwable cause) {
        super(cause);
    }
}
