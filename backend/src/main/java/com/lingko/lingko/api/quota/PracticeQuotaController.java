package com.lingko.lingko.api.quota;

import com.lingko.lingko.api.quota.dto.PracticeQuotaResponse;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 활성 로그인 세션에서 식별한 사용자의 연습 할당량을 제공한다.
 */
@RestController
@RequestMapping("/api/quota")
@RequiredArgsConstructor
public class PracticeQuotaController {

    private final PracticeQuotaService quotaService;
    private final ActiveSessionAuthenticator activeSessionAuthenticator;

    /**
     * 기존 endpoint 호환성을 유지하면서 활성 사용자의 현재 시간 충전형 평가 기회를 반환한다.
     */
    @GetMapping("/today")
    public PracticeQuotaResponse getTodayQuota(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return quotaService.getTodayQuota(activeSessionAuthenticator.authenticateBearer(authorization));
    }
}
