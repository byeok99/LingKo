package com.lingko.lingko.api.testsupport;

import com.lingko.lingko.api.auth.dto.AuthTokenResponse;
import com.lingko.lingko.api.legal.dto.LegalConsentRequest;
import com.lingko.lingko.core.domain.auth.service.AuthService;
import com.lingko.lingko.core.domain.legal.service.AiProcessingConsentService;
import com.lingko.lingko.core.domain.legal.service.LegalConsentService;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * E2E 테스트가 Google·Apple 로그인 화면을 거치지 않고 실제 로그인과 같은 형식의 세션을 받게 한다.
 *
 * <p>인증 없이 임의 사용자 세션을 발급하므로 {@code e2e} 프로필에서만 bean으로 등록된다.
 * 프로필이 없으면 경로 자체가 404이며, 운영 환경에는 이 프로필을 절대 켜지 않는다.</p>
 *
 * <p>{@code consented=true}(기본)는 고정 테스트 사용자를 재사용하고 약관·AI 처리 동의를 기록해 바로 평가 흐름을
 * 시험할 수 있게 한다. {@code false}는 동의 화면 흐름을 시험하도록 동의 기록 없는 새 사용자를 매번 만든다.</p>
 */
@Profile("e2e")
@RestController
@RequestMapping("/test-support")
@RequiredArgsConstructor
class E2eSessionController {

    static final String E2E_SOCIAL_ID = "e2e-user-1";

    private final UserRepository userRepository;
    private final AuthService authService;
    private final LegalConsentService legalConsentService;
    private final AiProcessingConsentService aiProcessingConsentService;
    private final Clock clock;

    @PostMapping("/sessions")
    public AuthTokenResponse issue(@RequestParam(defaultValue="true") boolean consented) {
        String socialId = consented ? E2E_SOCIAL_ID : "e2e-fresh-" + UUID.randomUUID();
        User user = userRepository
                .findBySocialIdAndSocialType(socialId, User.SocialType.GOOGLE)
                .orElseGet(() -> userRepository.save(User.builder()
                        .socialId(socialId)
                        .socialType(User.SocialType.GOOGLE)
                        .name("E2E User")
                        .build()));

        if (consented) {
            legalConsentService.record(user.getUserIdx(), new LegalConsentRequest(
                    true,
                    true,
                    false,
                    legalConsentService.getPolicy().documentVersion(),
                    clock.instant()));
            aiProcessingConsentService.record(
                    user.getUserIdx(), true, AiProcessingConsentService.CURRENT_VERSION);
        }
        return authService.loginReviewUser(user.getUserIdx());
    }
}