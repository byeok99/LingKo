package com.lingko.lingko.core.domain.quota.service;

import com.lingko.lingko.api.quota.dto.PracticeQuotaResponse;
import com.lingko.lingko.core.domain.quota.entity.DailyPracticeQuota;
import com.lingko.lingko.core.domain.quota.repository.DailyPracticeQuotaRepository;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E2E 사전조건을 위해 한 사용자의 평가 기회를 원하는 값으로 다시 만든다.
 *
 * <p>운영 흐름에는 필요 없는 상태 조작이라 {@code e2e} 프로필에서만 bean으로 등록한다.
 * domain entity에 테스트 전용 setter를 두지 않으려고, 기존 quota 행을 지우고 원하는 상태의 행을 새로 만든다.
 * 그래서 진행 중 평가가 잡아 둔 예약도 함께 사라지며, 그 평가가 끝날 때의 확정이나 복구는 실패할 수 있다.</p>
 */
@Profile("e2e")
@Service
@RequiredArgsConstructor
public class E2eQuotaSetupService {

    private final UserRepository userRepository;
    private final DailyPracticeQuotaRepository quotaRepository;
    private final PracticeQuotaService practiceQuotaService;
    private final Clock clock;

    /**
     * 남은 자연 충전 기회를 {@code remaining}(0~5)회로 맞추고, 앱의 {@code GET /api/quota/today}와 같은 응답을 돌려준다.
     *
     * <p>5회 미만이면 지금부터 1시간 뒤를 다음 충전 시각으로 둔다. 실제로 사용한 직후와 같은 상태라서
     * 카운트다운이 60분부터 시작한다. 5회면 충전 대기가 없으므로 {@code nextRefillAt}은 null이다.
     * 광고 보상 기회는 0으로 둔다.</p>
     */
    @Transactional
    public PracticeQuotaResponse setRemaining(Long userId, int remaining) {
        int limit = PracticeQuotaService.MAX_NATURAL_PRACTICES;
        if (remaining < 0 || remaining > limit) {
            throw new IllegalArgumentException("remaining must be between 0 and " + limit);
        }
        // 다른 quota 경로와 같은 user → quota 순서로 잠근다. quota부터 지우면, 같은 순간 앱의 quota 조회가
        // user 잠금을 쥔 채 quota를 기다리면서 deadlock이 난다(PracticeQuotaService.findOrCreateCurrentQuota 참고).
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("user not found: " + userId));

        quotaRepository.deleteAllByUserId(userId);
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
        // 직접 만든 값이 아니라 앱과 같은 조회 경로의 결과를 돌려줘야 E2E가 화면과 서버 값을 그대로 비교할 수 있다
        return practiceQuotaService.getTodayQuota(userId);
    }

    /**
     * 남은 기회는 그대로 두고 다음 자연 충전 시각만 지금부터 {@code fromNow} 뒤로 옮긴 뒤, 앱과 같은 조회 결과를 돌려준다.
     *
     * <p>"1시간을 기다린 뒤" 상태를 실제로 기다리지 않고 만들기 위한 것이다. {@code fromNow}가 음수면 충전 시각이 이미
     * 지난 상태가 되고, 돌려주는 조회에서 운영과 같은 {@code replenishAt} 규칙으로 충전이 반영된다. 충전 계산을 여기서
     * 흉내 내지 않고 운영 경로에 맡겨야 TC가 실제 충전 규칙을 검증한다.</p>
     *
     * <p>충전 대기가 없으면(quota 행이 없거나 5회 보유라 {@code nextRefillAt}이 null) 옮길 시각이 없으므로 empty다.
     * {@link #setRemaining}과 같은 이유로 행을 새로 만들기 때문에 진행 중 평가의 예약은 사라진다.</p>
     */
    @Transactional
    public Optional<PracticeQuotaResponse> moveNextRefill(Long userId, Duration fromNow) {
        // setRemaining과 같은 user → quota 잠금 순서를 지킨다
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("user not found: " + userId));
        // 충전을 반영하기 전의 저장값을 그대로 읽는다. 여기서 충전하면 옮기기 전에 이미 기회가 바뀐다
        Optional<DailyPracticeQuota> current = quotaRepository.findCurrentByUserForUpdate(userId)
                .filter(quota -> quota.getNextRefillAt() != null);
        if (current.isEmpty()) {
            return Optional.empty();
        }
        DailyPracticeQuota quota = current.get();

        quotaRepository.deleteAllByUserId(userId);
        quotaRepository.saveAndFlush(DailyPracticeQuota.builder()
                .user(user)
                .quotaDate(quota.getQuotaDate())
                .freeLimit(quota.getFreeLimit())
                .freeUsed(quota.getFreeUsed())
                .rewardedAvailable(quota.getRewardedAvailable())
                .nextRefillAt(clock.instant().plus(fromNow))
                .build());
        return Optional.of(practiceQuotaService.getTodayQuota(userId));
    }
}
