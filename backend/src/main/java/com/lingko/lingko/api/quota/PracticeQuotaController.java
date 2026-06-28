package com.lingko.lingko.api.quota;

import com.lingko.lingko.api.quota.dto.PracticeQuotaResponse;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.service.JwtTokenProvider;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quota")
@RequiredArgsConstructor
public class PracticeQuotaController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final PracticeQuotaService quotaService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/today")
    public PracticeQuotaResponse getTodayQuota(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return quotaService.getTodayQuota(resolveUserId(authorization));
    }

    private Long resolveUserId(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new AuthException("Missing bearer token");
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new AuthException("Missing bearer token");
        }

        return jwtTokenProvider.parseAccessTokenUserId(token);
    }
}
