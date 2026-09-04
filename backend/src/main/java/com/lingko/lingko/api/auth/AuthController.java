package com.lingko.lingko.api.auth;

import com.lingko.lingko.api.auth.dto.AuthTokenResponse;
import com.lingko.lingko.api.auth.dto.OAuthLoginRequest;
import com.lingko.lingko.api.auth.dto.ReviewLoginRequest;
import com.lingko.lingko.api.auth.dto.RefreshTokenRequest;
import com.lingko.lingko.core.domain.auth.service.AuthService;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.user.service.AccountDeletionService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인, 토큰 회전, 현재 기기 로그아웃과 회원 탈퇴 HTTP 연산을 제공한다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ActiveSessionAuthenticator activeSessionAuthenticator;
    private final AccountDeletionService accountDeletionService;
    private final ReviewAccessGuard reviewAccessGuard;

    /**
     * 검증된 외부 provider 신원 토큰을 LingKo 세션으로 교환한다.
     */
    @PostMapping("/oauth/login")
    public AuthTokenResponse loginWithOAuth(@Valid @RequestBody OAuthLoginRequest request) {
        return authService.loginWithOAuth(request);
    }

    /** Review Notes로 전달된 코드를 검증한 뒤 미리 준비된 제한 계정에 새 세션을 발급한다. */
    @PostMapping("/review/login")
    public AuthTokenResponse loginForReview(
            @Valid @RequestBody ReviewLoginRequest request,
            HttpServletRequest servletRequest
    ) {
        Long reviewUserId = reviewAccessGuard.authorizeAndConsume(
                request.trimmedAccessCode(),
                servletRequest.getRemoteAddr()
        );
        return authService.loginReviewUser(reviewUserId);
    }

    /**
     * 호출자의 현재 갱신 토큰을 회전하고 대체 토큰 쌍을 반환한다.
     */
    @PostMapping("/token/refresh")
    public AuthTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    /**
     * 전달된 갱신 토큰이 나타내는 기기 세션을 폐기한다.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
    }

    /**
     * 현재 Access·Refresh Token을 모두 확인한 뒤 사용자 소유 개인정보와 음성을 삭제한다.
     */
    @DeleteMapping("/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        Long userId = activeSessionAuthenticator.authenticateBearer(authorization);
        accountDeletionService.deleteAccount(userId, request);
    }
}
