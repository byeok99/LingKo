package com.lingko.lingko.core.domain.user.service;

/**
 * 원격 개인정보 정리가 완료되지 않아 계정 삭제를 안전하게 재시도해야 함을 나타낸다.
 */
public class AccountDeletionUnavailableException extends RuntimeException {

    public AccountDeletionUnavailableException(Throwable cause) {
        super("Account deletion is temporarily unavailable", cause);
    }
}
