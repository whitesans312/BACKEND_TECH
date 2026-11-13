package com.parkflow.billing.shared;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
