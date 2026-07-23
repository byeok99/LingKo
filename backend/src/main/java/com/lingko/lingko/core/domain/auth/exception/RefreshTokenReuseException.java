package com.lingko.lingko.core.domain.auth.exception;

public class RefreshTokenReuseException extends AuthException {
    public RefreshTokenReuseException() {
        super("Refresh token reuse detected");
    }
}
