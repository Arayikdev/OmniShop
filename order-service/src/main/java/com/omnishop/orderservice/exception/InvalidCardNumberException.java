package com.omnishop.orderservice.exception;

public class InvalidCardNumberException extends RuntimeException {
    public InvalidCardNumberException(String message) { super(message); }
}
