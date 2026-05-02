package com.omnishop.orderservice.exception;

public class OrderNotCancellableException extends RuntimeException {
    public OrderNotCancellableException(String message) { super(message); }
}
