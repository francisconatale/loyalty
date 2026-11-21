package com.loyalty.shared.exception;

public class InvalidAccountStateException extends RuntimeException {
    public InvalidAccountStateException(String message) {
        super(message);
    }
}
