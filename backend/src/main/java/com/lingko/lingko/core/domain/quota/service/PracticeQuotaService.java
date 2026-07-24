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
        return toResponse(findOrCreateTodayQuota(userId));
    }

    @Transactional
    public PracticeQuotaResponse consumePractice(Long userId) {
        // 호출자가 차감 전 시점 데이터을 받지 않도록 조회와 변경을 하나의 트랜잭션에서 처리한다.
        DailyPracticeQuota quota = findOrCreateTodayQuota(userId);
        if (!quota.hasRemainingPractice()) {
            throw new QuotaExceededException("Daily practice quota exceeded");
        }

        quota.consumePractice();
        return toResponse(quota);
    }

    @Transactional
    public PracticeQuotaReservation reservePractice(Long userId) {
        DailyPracticeQuota quota = findOrCreateTodayQuota(userId);
        if (!quota.hasRemainingPractice()) {
            throw new QuotaExceededException("Daily practice quota exceeded");
        }

        DailyPracticeQuota.ReservationSource source = quota.reservePractice();
        return new PracticeQuotaReservation(
                userId,
                quota.getQuotaDate(),
                QuotaSource.valueOf(source.name())
        );
    }

    @Transactional
    public void confirmPractice(PracticeQuotaReservation reservation) {
        DailyPracticeQuota quota = findReservationQuota(reservation);
        quota.confirmReservation(DailyPracticeQuota.ReservationSource.valueOf(reservation.source().name()));
    }

    @Transactional
    public void releasePractice(PracticeQuotaReservation reservation) {
        DailyPracticeQuota quota = findReservationQuota(reservation);
        quota.releaseReservation(DailyPracticeQuota.ReservationSource.valueOf(reservation.source().name()));
    }

    private DailyPracticeQuota findOrCreateTodayQuota(Long userId) {
        LocalDate today = today();
        return quotaRepository.findByUserUserIdxAndQuotaDate(userId, today)
                .orElseGet(() -> quotaRepository.save(DailyPracticeQuota.create(
                        findAuthenticatedUser(userId),
                        today,
                        DAILY_FREE_LIMIT
                )));
    }

    private User findAuthenticatedUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Authenticated user not found"));
    }

    private DailyPracticeQuota findReservationQuota(PracticeQuotaReservation reservation) {
        if (reservation == null
                || reservation.userId() == null
                || reservation.quotaDate() == null
                || reservation.source() == null) {
            throw new IllegalArgumentException("quota reservation must be complete");
        }

        return quotaRepository.findByUserUserIdxAndQuotaDate(
                        reservation.userId(),
                        reservation.quotaDate()
                )
                .orElseThrow(() -> new IllegalStateException("quota reservation does not exist"));
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
