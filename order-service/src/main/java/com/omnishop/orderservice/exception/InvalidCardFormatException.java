package com.omnishop.orderservice.exception;

public class InvalidCardFormatException extends RuntimeException {
    public InvalidCardFormatException(String message) { super(message); }
}
