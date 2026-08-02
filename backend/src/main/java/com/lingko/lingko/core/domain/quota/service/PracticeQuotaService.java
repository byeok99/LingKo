package com.lingko.lingko.core.domain.quota.service;

import com.lingko.lingko.api.quota.dto.PracticeQuotaResponse;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.quota.entity.DailyPracticeQuota;
import com.lingko.lingko.core.domain.quota.exception.QuotaExceededException;
import com.lingko.lingko.core.domain.quota.repository.DailyPracticeQuotaRepository;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
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

    public static final int DAILY_FREE_LIMIT = 5;
    // 서버 배포 시간대와 무관하게 한국 서비스 날짜를 기준으로 할당량을 초기화한다.
    public static final ZoneId RESET_ZONE = ZoneId.of("Asia/Seoul");

    private final DailyPracticeQuotaRepository quotaRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public PracticeQuotaService(
            DailyPracticeQuotaRepository quotaRepository,
            UserRepository userRepository,
            ObjectProvider<Clock> clockProvider
    ) {
        this.quotaRepository = quotaRepository;
        this.userRepository = userRepository;
        this.clock = clockProvider.getIfAvailable(() -> Clock.system(RESET_ZONE));
    }

    @Transactional
    public PracticeQuotaResponse getTodayQuota(Long userId) {
        LocalDate today = today();
        return toResponse(findOrCreateQuota(userId, today));
    }

    @Transactional
    public PracticeQuotaResponse consumePractice(Long userId) {
        PracticeQuotaReservation reservation = reservePractice(userId);
        confirmPractice(reservation);
        return toResponse(findQuota(userId, reservation.quotaDate()));
    }

    @Transactional
    public PracticeQuotaReservation reservePractice(Long userId) {
        LocalDate today = today();
        findOrCreateQuota(userId, today);

        if (quotaRepository.reserveFreePractice(userId, today) == 1) {
            return new PracticeQuotaReservation(userId, today, QuotaSource.FREE);
        }
        if (quotaRepository.reserveRewardedPractice(userId, today) == 1) {
            return new PracticeQuotaReservation(userId, today, QuotaSource.REWARDED);
        }

        // 조건부 UPDATE가 0이면 다른 요청이 먼저 마지막 횟수를 확보했거나 이미 소진된 상태다.
        throw new QuotaExceededException("Daily practice quota exceeded");
    }

    @Transactional
    public void confirmPractice(PracticeQuotaReservation reservation) {
        validateReservation(reservation);
        int updated = reservation.source() == QuotaSource.FREE
                ? quotaRepository.confirmFreePractice(reservation.userId(), reservation.quotaDate())
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

    private DailyPracticeQuota findOrCreateQuota(Long userId, LocalDate quotaDate) {
        Optional<DailyPracticeQuota> existingQuota =
                quotaRepository.findByUserUserIdxAndQuotaDate(userId, quotaDate);
        if (existingQuota.isPresent()) {
            return existingQuota.get();
        }

        // 잠글 quota 행이 없는 최초 생성 경쟁은 항상 존재하는 user 행으로 직렬화한다.
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AuthException("Authenticated user not found"));
        // Repeatable Read snapshot 재조회 대신 locking read 결과를 그대로 사용해야 앞선 transaction의 생성을 볼 수 있다.
        return quotaRepository.findByUserAndDateForUpdate(userId, quotaDate)
                .orElseGet(() -> quotaRepository.save(DailyPracticeQuota.create(
                        user,
                        quotaDate,
                        DAILY_FREE_LIMIT
                )));
    }

    private DailyPracticeQuota findQuota(Long userId, LocalDate quotaDate) {
        return quotaRepository.findByUserUserIdxAndQuotaDate(userId, quotaDate)
                .orElseThrow(() -> new IllegalStateException("daily practice quota does not exist"));
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
        return new PracticeQuotaResponse(
                quota.getQuotaDate(),
                quota.getFreeLimit(),
                quota.getFreeUsed(),
                quota.getRewardedAvailable(),
                quota.remainingPractices(),
                resetAt(quota.getQuotaDate())
        );
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(RESET_ZONE));
    }

    private OffsetDateTime resetAt(LocalDate quotaDate) {
        return quotaDate.plusDays(1)
                .atStartOfDay(RESET_ZONE)
                .toOffsetDateTime();
    }

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
