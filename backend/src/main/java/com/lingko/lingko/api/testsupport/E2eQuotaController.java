package com.lingko.lingko.api.testsupport;

import com.lingko.lingko.api.quota.dto.PracticeQuotaResponse;
import com.lingko.lingko.core.domain.quota.entity.DailyPracticeQuota;
import com.lingko.lingko.core.domain.quota.repository.DailyPracticeQuotaRepository;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * E2E 테스트가 고정 테스트 사용자({@code e2e-user-1})의 평가 기회를 원하는 값으로 맞추게 한다.
 *
 * <p>평가 기회는 이전 실행의 평가·충전 결과가 누적되므로, "기회 3회 계정" 같은 사전조건을 테스트마다
 * 다시 만들어야 결과가 실행 순서에 흔들리지 않는다. 세션 endpoint와 같은 이유로 {@code e2e} 프로필에서만
 * bean으로 등록되며 프로필이 없으면 경로가 404다.</p>
 *
 * <p>domain에 테스트 전용 setter를 두지 않으려고 기존 행을 모두 지우고 원하는 상태의 행을 새로 만든다.
 * 그래서 진행 중 평가가 잡아 둔 예약도 함께 사라지며, 그 평가가 끝날 때의 확정·복구는 실패할 수 있다.
 * 평가가 진행 중이지 않을 때만 호출한다.</p>
 */
@Profile("e2e")
@RestController
@RequestMapping("/test-support")
@RequiredArgsConstructor
class E2eQuotaController {

    private final UserRepository userRepository;
    private final DailyPracticeQuotaRepository quotaRepository;
    private final PracticeQuotaService practiceQuotaService;
    private final Clock clock;

    /**
     * 남은 자연 충전 기회를 {@code remaining}(0~5)회로 맞추고 앱이 받는 것과 같은 응답을 돌려준다.
     *
     * <p>5회 미만이면 지금부터 1시간 뒤를 다음 충전 시각으로 둔다. 실제 사용 직후와 같은 상태라서
     * 카운트다운이 60분에서 시작한다. 5회면 충전 대기가 없으므로 {@code nextRefillAt}은 null이다.
     * 광고 보상 기회는 0으로 둔다. 고정 사용자가 아직 없으면, 즉 세션을 한 번도 발급하지 않았으면 404다.</p>
     */
    @PutMapping("/quota")
    @Transactional
    public ResponseEntity<PracticeQuotaResponse> setQuota(@RequestParam @Min(0) @Max(5) int remaining) {
        User user = userRepository
                .findBySocialIdAndSocialType(E2eSessionController.E2E_SOCIAL_ID, User.SocialType.GOOGLE)
                .orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        int limit = PracticeQuotaService.MAX_NATURAL_PRACTICES;
        quotaRepository.deleteAllByUserId(user.getUserIdx());
        quotaRepository.saveAndFlush(DailyPracticeQuota.builder()
                .user(user)
                .quotaDate(LocalDate.now(clock.withZone(PracticeQuotaService.SERVICE_ZONE)))
                .freeLimit(limit)
                .freeUsed(limit - remaining)
                .rewardedAvailable(0)
                .nextRefillAt(remaining < limit
                        ? clock.instant().plus(PracticeQuotaService.REFILL_INTERVAL)
                        : null)
                .build());
        // 직접 만든 값 대신 앱과 같은 조회 경로의 결과를 돌려줘 E2E가 화면과 서버 값을 그대로 비교하게 한다
        return ResponseEntity.ok(practiceQuotaService.getTodayQuota(user.getUserIdx()));
    }
}
