package com.parkflow.billing.shared;

public class UnprocessableOperationException extends RuntimeException {
    public UnprocessableOperationException(String message) { super(message); }
}
