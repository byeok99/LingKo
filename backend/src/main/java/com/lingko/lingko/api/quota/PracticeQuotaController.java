package com.lingko.lingko.api.quota;

import com.lingko.lingko.api.quota.dto.PracticeQuotaResponse;
import com.lingko.lingko.api.quota.dto.AdRewardSessionResponse;
import com.lingko.lingko.api.quota.dto.AdRewardSessionStatusResponse;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import com.lingko.lingko.core.domain.quota.service.AdRewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 활성 로그인 세션에서 식별한 사용자의 연습 할당량을 제공한다.
 */
@RestController
@RequestMapping("/api/quota")
@RequiredArgsConstructor
public class PracticeQuotaController {

    private final PracticeQuotaService quotaService;
    private final ActiveSessionAuthenticator activeSessionAuthenticator;
    private final AdRewardService adRewardService;

    /**
     * 기존 endpoint 호환성을 유지하면서 활성 사용자의 현재 시간 충전형 평가 기회를 반환한다.
     */
    @GetMapping("/today")
    public PracticeQuotaResponse getTodayQuota(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return quotaService.getTodayQuota(activeSessionAuthenticator.authenticateBearer(authorization));
    }

    /**
     * Mobile Ads SDK의 reward callback 한 건을 현재 사용자의 평가 기회 1회로 교환한다.
     * 지급량과 사용자 ID는 입력에서 받지 않아 서버 정책과 인증 경계를 우회할 수 없게 한다.
     */
    @PostMapping("/ad-reward-sessions")
    public AdRewardSessionResponse createAdRewardSession(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Long userId = activeSessionAuthenticator.authenticateBearer(authorization);
        return adRewardService.createSession(userId);
    }

    /** token을 보유한 다른 사용자가 처리 상태를 조회하지 못하게 인증 user와 함께 조회한다. */
    @GetMapping("/ad-reward-sessions/{sessionToken}")
    public AdRewardSessionStatusResponse getAdRewardSessionStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String sessionToken
    ) {
        Long userId = activeSessionAuthenticator.authenticateBearer(authorization);
        return adRewardService.getSessionStatus(userId, sessionToken);
    }

    /** 오래된 앱의 client-generated event 지급 경로는 보안상 복구하지 않고 명시적으로 폐기한다. */
    @PostMapping("/ad-rewards")
    public ResponseEntity<Void> rejectLegacyClientRewardClaim() {
        return ResponseEntity.status(HttpStatus.GONE).build();
    }
}
