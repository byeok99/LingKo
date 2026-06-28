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

@Service
public class PracticeQuotaService {

    public static final int DAILY_FREE_LIMIT = 5;
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
        DailyPracticeQuota quota = findOrCreateTodayQuota(userId);
        if (!quota.hasRemainingPractice()) {
            throw new QuotaExceededException("Daily practice quota exceeded");
        }

        quota.consumePractice();
        return toResponse(quota);
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
}
