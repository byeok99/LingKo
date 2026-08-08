package com.lingko.lingko.api.legal;

import com.lingko.lingko.api.legal.dto.LegalConsentRequest;
import com.lingko.lingko.api.legal.dto.LegalConsentStatusResponse;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.legal.service.LegalConsentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 활성 로그인 사용자에게 현재 약관 동의 상태 조회와 제출 endpoint를 제공한다.
 */
@RestController
@RequestMapping("/api/legal/consent")
@RequiredArgsConstructor
public class LegalConsentController {

    private final LegalConsentService legalConsentService;
    private final ActiveSessionAuthenticator activeSessionAuthenticator;

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
