package com.lingko.lingko.core.domain.quota.exception;

public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(String message) {
        super(message);
    }
}
