package com.lingko.lingko.api.legal;

import com.lingko.lingko.api.legal.dto.LegalConsentRequest;
import com.lingko.lingko.api.legal.dto.LegalConsentPolicyResponse;
import com.lingko.lingko.api.legal.dto.LegalConsentStatusResponse;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.legal.service.LegalConsentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인 전 현재 정책 조회와 인증 사용자의 약관 동의 상태·제출 endpoint를 제공한다.
 */
@RestController
@RequestMapping("/api/legal/consent")
@RequiredArgsConstructor
public class LegalConsentController {

    private final LegalConsentService legalConsentService;
    private final ActiveSessionAuthenticator activeSessionAuthenticator;

    /**
     * 로그인 전에 표시할 현재 문서 버전을 공개하며 중간 cache의 구버전 재사용을 막는다.
     *
     * <p>문서 버전은 공개 정보이고 사용자 상태를 포함하지 않으므로 Bearer 인증을 요구하지 않는다.</p>
     */
    @GetMapping("/policy")
    public ResponseEntity<LegalConsentPolicyResponse> getPolicy() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(legalConsentService.getPolicy());
    }

    /** 현재 문서 버전의 재동의 필요 여부를 반환한다. */
    @GetMapping
    public LegalConsentStatusResponse getStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Long userId = activeSessionAuthenticator.authenticateBearer(authorization);
        return legalConsentService.getStatus(userId);
    }

    /**
     * body의 사용자 식별값을 신뢰하지 않고 Bearer token 소유자에게 동의를 귀속한다.
     */
    @PostMapping
    public LegalConsentStatusResponse record(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody LegalConsentRequest request
    ) {
        Long userId = activeSessionAuthenticator.authenticateBearer(authorization);
        return legalConsentService.record(userId, request);
    }
}
