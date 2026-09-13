package com.example.bankapi.exception;

/** Thrown for business-rule violations that aren't simple bean-validation failures. */
public class InvalidTransactionException extends RuntimeException {

    public InvalidTransactionException(String message) {
        super(message);
    }
}
