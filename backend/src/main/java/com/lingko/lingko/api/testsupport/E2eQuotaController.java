package com.lingko.lingko.api.testsupport;

import com.lingko.lingko.api.quota.dto.PracticeQuotaResponse;
import com.lingko.lingko.core.domain.quota.service.E2eQuotaSetupService;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * E2E 테스트가 고정 테스트 사용자({@code e2e-user-1})의 평가 기회를 원하는 값으로 맞추게 한다.
 *
 * <p>평가 기회는 이전 실행의 평가·충전 결과가 누적된다. 그래서 "기회 3회 계정" 같은 사전조건을 테스트마다
 * 다시 만들어야 결과가 실행 순서에 흔들리지 않는다. 세션 endpoint와 같은 이유로 {@code e2e} 프로필에서만
 * bean으로 등록되며, 프로필이 없으면 경로가 404다. 상태를 바꾸는 규칙은 {@link E2eQuotaSetupService}가 맡는다.</p>
 */
@Profile("e2e")
@RestController
@RequestMapping("/test-support")
@RequiredArgsConstructor
class E2eQuotaController {

    private final UserRepository userRepository;
    private final E2eQuotaSetupService quotaSetupService;

    /**
     * 남은 자연 충전 기회를 {@code remaining}(0~5)회로 맞춘다. 범위 밖 값은 400이다.
     *
     * <p>고정 사용자가 아직 없으면, 즉 세션을 한 번도 발급하지 않았으면 409다. 404로 응답하면
     * 프로필이 꺼진 경우와 구분되지 않아, E2E가 실패했을 때 원인을 알 수 없다.</p>
     */
    @PutMapping("/quota")
    public ResponseEntity<PracticeQuotaResponse> setQuota(@RequestParam @Min(0) @Max(5) int remaining) {
        return userRepository
                .findBySocialIdAndSocialType(E2eSessionController.E2E_SOCIAL_ID, User.SocialType.GOOGLE)
                .map(user -> ResponseEntity.ok(quotaSetupService.setRemaining(user.getUserIdx(), remaining)))
                .orElseGet(() -> ResponseEntity.status(409).build());
    }
}
