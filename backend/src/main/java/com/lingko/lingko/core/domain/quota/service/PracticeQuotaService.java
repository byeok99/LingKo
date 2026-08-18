package com.lingko.lingko.core.domain.quota.service;

import com.lingko.lingko.api.quota.dto.PracticeQuotaResponse;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.quota.entity.DailyPracticeQuota;
import com.lingko.lingko.core.domain.quota.exception.QuotaExceededException;
import com.lingko.lingko.core.domain.quota.repository.DailyPracticeQuotaRepository;
import com.lingko.lingko.core.domain.quota.entity.AdRewardReceipt;
import com.lingko.lingko.core.domain.quota.repository.AdRewardReceiptRepository;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Practice 할당량 업무 규칙을 조율한다.
 *
 * 컨트롤러와 외부 어댑터가 정책을 소유하지 않도록 도메인 서비스에 조율을 집중했다.
 */
@Service
public class PracticeQuotaService {

    public static final int MAX_NATURAL_PRACTICES = 5;
    public static final Duration REFILL_INTERVAL = Duration.ofHours(1);
    // 서버 배포 시간대와 무관하게 API 날짜와 offset을 일관된 서비스 시간대로 제공한다.
    public static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final DailyPracticeQuotaRepository quotaRepository;
    private final UserRepository userRepository;
    private final AdRewardReceiptRepository adRewardReceiptRepository;
    private final Clock clock;

    public PracticeQuotaService(
            DailyPracticeQuotaRepository quotaRepository,
            UserRepository userRepository,
            AdRewardReceiptRepository adRewardReceiptRepository,
            ObjectProvider<Clock> clockProvider
    ) {
        this.quotaRepository = quotaRepository;
        this.userRepository = userRepository;
        this.adRewardReceiptRepository = adRewardReceiptRepository;
        this.clock = clockProvider.getIfAvailable(() -> Clock.system(SERVICE_ZONE));
    }

    @Transactional
    public PracticeQuotaResponse getTodayQuota(Long userId) {
        return toResponse(findOrCreateCurrentQuota(userId));
    }

    @Transactional
    public PracticeQuotaResponse consumePractice(Long userId) {
        PracticeQuotaReservation reservation = reservePractice(userId);
        confirmPractice(reservation);
        return toResponse(findCurrentQuota(userId));
    }

    /**
     * 광고 SDK가 획득을 확정한 event 하나를 평가 기회 1회로 바꾼다.
     *
     * <p>quota 행을 먼저 잠가 같은 사용자의 병렬 지급을 직렬화한 뒤 event 기록을 확인한다.
     * 동일 event 재전송은 현재 상태만 반환하며, 자연 충전 timer는 수정하지 않는다.</p>
     */
    @Transactional
    public boolean grantVerifiedAdReward(Long userId, String rewardEventId) {
        DailyPracticeQuota quota = findOrCreateCurrentQuota(userId);
        if (adRewardReceiptRepository.existsByProviderTransactionId(rewardEventId)) {
            return false;
        }

        adRewardReceiptRepository.save(AdRewardReceipt.create(userId, rewardEventId));
        // 버튼을 누른 뒤 자연 충전으로 이미 5회가 된 경쟁 상황에서도 최대치를 넘기지 않는다.
        if (quota.remainingPractices() < MAX_NATURAL_PRACTICES) {
            quota.addRewardedPractices(1);
            return true;
        }
        return false;
    }

    @Transactional
    public PracticeQuotaReservation reservePractice(Long userId) {
        LocalDate today = today();
        DailyPracticeQuota quota = findOrCreateCurrentQuota(userId);
        LocalDate quotaDate = quota.getQuotaDate();

        if (quotaRepository.reserveFreePractice(
                userId,
                quotaDate,
                clock.instant().plus(REFILL_INTERVAL)
        ) == 1) {
            return new PracticeQuotaReservation(userId, quotaDate, QuotaSource.FREE);
        }
        if (quotaRepository.reserveRewardedPractice(userId, quotaDate) == 1) {
            return new PracticeQuotaReservation(userId, quotaDate, QuotaSource.REWARDED);
        }

        // 조건부 UPDATE가 0이면 다른 요청이 먼저 마지막 횟수를 확보했거나 이미 소진된 상태다.
        throw new QuotaExceededException("Practice energy exhausted");
    }

    @Transactional
    public void confirmPractice(PracticeQuotaReservation reservation) {
        validateReservation(reservation);
        int updated = reservation.source() == QuotaSource.FREE
                ? quotaRepository.confirmFreePractice(
                        reservation.userId(),
                        reservation.quotaDate(),
                        clock.instant().plus(REFILL_INTERVAL)
                )
                : quotaRepository.confirmRewardedPractice(reservation.userId(), reservation.quotaDate());
        requireReservationUpdate(updated);
    }

    @Transactional
    public void releasePractice(PracticeQuotaReservation reservation) {
        requireReservationUpdate(releaseReservedPractice(reservation));
    }

    /**
     * 최종 실패 복구에서 예약이 이미 사라진 비정상 상태를 terminal 작업 롤백 없이 판별한다.
     */
    @Transactional
    public boolean releasePracticeIfReserved(PracticeQuotaReservation reservation) {
        return releaseReservedPractice(reservation) == 1;
    }

    private int releaseReservedPractice(PracticeQuotaReservation reservation) {
        validateReservation(reservation);
        return reservation.source() == QuotaSource.FREE
                ? quotaRepository.releaseFreePractice(reservation.userId(), reservation.quotaDate())
                : quotaRepository.releaseRewardedPractice(reservation.userId(), reservation.quotaDate());
    }

    private DailyPracticeQuota findOrCreateCurrentQuota(Long userId) {
        Optional<DailyPracticeQuota> existingQuota = quotaRepository.findCurrentByUserForUpdate(userId);
        if (existingQuota.isPresent()) {
            return replenish(existingQuota.get());
        }

        // 잠글 energy 행이 없는 최초 생성 경쟁은 항상 존재하는 user 행으로 직렬화한다.
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AuthException("Authenticated user not found"));
        // Repeatable Read snapshot 재조회 대신 locking read로 앞선 transaction이 만든 현재 행을 확인한다.
        return quotaRepository.findCurrentByUserForUpdate(userId)
                .map(this::replenish)
                .orElseGet(() -> quotaRepository.save(DailyPracticeQuota.create(
                        user,
                        today(),
                        MAX_NATURAL_PRACTICES
                )));
    }

    private DailyPracticeQuota findCurrentQuota(Long userId) {
        return quotaRepository.findCurrentByUserForUpdate(userId)
                .map(this::replenish)
                .orElseThrow(() -> new IllegalStateException("practice energy does not exist"));
    }

    private DailyPracticeQuota replenish(DailyPracticeQuota quota) {
        quota.replenishAt(clock.instant(), REFILL_INTERVAL);
        return quota;
    }

    private void validateReservation(PracticeQuotaReservation reservation) {
        if (reservation == null
                || reservation.userId() == null
                || reservation.quotaDate() == null
                || reservation.source() == null) {
            throw new IllegalArgumentException("quota reservation must be complete");
        }
    }

    private void requireReservationUpdate(int updated) {
        if (updated != 1) {
            throw new IllegalStateException("quota reservation does not exist");
        }
    }

    private PracticeQuotaResponse toResponse(DailyPracticeQuota quota) {
        Instant now = clock.instant();
        return new PracticeQuotaResponse(
                today(),
                quota.getFreeLimit(),
                quota.getFreeUsed(),
                quota.getRewardedAvailable(),
                quota.remainingPractices(),
                toOffsetDateTime(quota.getNextRefillAt()),
                toOffsetDateTime(now)
        );
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(SERVICE_ZONE));
    }

    private OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, SERVICE_ZONE);
    }

    /** 한 번의 연습이 무료 기본량과 광고 보상량 중 어디에서 예약됐는지 나타낸다. */
    public enum QuotaSource {
        FREE,
        REWARDED
    }

    /**
     * 날짜가 바뀐 뒤 보상하더라도 원래 예약한 일자의 정확한 횟수를 복구하기 위한 내부 token이다.
     */
    public record PracticeQuotaReservation(
            Long userId,
            LocalDate quotaDate,
            QuotaSource source
    ) {
    }
}
