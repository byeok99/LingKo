package com.lingko.lingko.core.domain.auth.exception;

/**
 * 외부 OAuth 공급자의 공개 키를 가져오지 못해 신원 검증을 끝낼 수 없는 일시 장애를 나타낸다.
 *
 * <p>사용자 token이 잘못된 {@link AuthException}(401)과 구분해 503으로 응답한다. 같은 401로 보내면
 * 앱이 정상 token을 버리고 재로그인을 반복하게 되므로, 호출자는 이 예외를 재시도 가능한 실패로 해석한다.</p>
 */
public class OAuthProviderUnavailableException extends RuntimeException {

    public OAuthProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
