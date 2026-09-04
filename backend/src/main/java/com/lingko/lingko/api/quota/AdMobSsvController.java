package com.lingko.lingko.api.quota;

import com.lingko.lingko.core.domain.quota.service.AdRewardService;
import com.lingko.lingko.infra.advertising.AdMobSsvVerifier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Google AdMob만 호출하는 signed server-side reward callback 경계다. */
@RestController
@RequiredArgsConstructor
public class AdMobSsvController {

    private final AdMobSsvVerifier verifier;
    private final AdRewardService adRewardService;

    @GetMapping("/api/quota/ad-rewards/ssv")
    public ResponseEntity<Void> receive(HttpServletRequest request) {
        // Servlet이 decoding한 parameter map 대신 raw query를 전달해야 Google 서명 원문이 보존된다.
        adRewardService.processVerifiedCallback(verifier.verify(request.getQueryString()));
        return ResponseEntity.ok().build();
    }
}
