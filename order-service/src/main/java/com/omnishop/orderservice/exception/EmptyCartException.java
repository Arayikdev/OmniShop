package com.omnishop.orderservice.exception;

public class EmptyCartException extends RuntimeException {
    public EmptyCartException(String message) { super(message); }
}
