package com.lingko.lingko.api.legal;

import com.lingko.lingko.api.legal.dto.AiProcessingConsentRequest;
import com.lingko.lingko.api.legal.dto.AiProcessingConsentResponse;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.legal.service.AiProcessingConsentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 인증 사용자 자신의 AI 전송 허용 상태 조회와 명시적 허용·철회를 제공한다. */
@RestController
@RequestMapping("/api/legal/ai-consent")
@RequiredArgsConstructor
public class AiProcessingConsentController {
    private final ActiveSessionAuthenticator authenticator;
    private final AiProcessingConsentService service;

    @GetMapping
    public AiProcessingConsentResponse get(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return service.getStatus(authenticator.authenticateBearer(authorization));
    }

    @PostMapping
    public AiProcessingConsentResponse record(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody AiProcessingConsentRequest request) {
        return service.record(authenticator.authenticateBearer(authorization), request.granted(), request.noticeVersion());
    }
}
