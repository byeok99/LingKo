package com.lingko.lingko.core.domain.auth.exception;

/**
 * 이미 회전된 갱신 토큰이 다시 제출됐음을 나타낸다.
 *
 * <p>전용 예외 유형을 사용해 트랜잭션이 세션 폐기를 커밋하면서도
 * 호출자에게 인증 실패를 반환할 수 있게 한다.</p>
 */
public class RefreshTokenReuseException extends AuthException {
    public RefreshTokenReuseException() {
        super("Refresh token reuse detected");
    }
}
