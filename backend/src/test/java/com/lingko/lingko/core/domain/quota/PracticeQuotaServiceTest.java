package com.lingko.lingko.core.domain.quota;

import com.lingko.lingko.api.quota.dto.PracticeQuotaResponse;
import com.lingko.lingko.core.domain.quota.entity.DailyPracticeQuota;
import com.lingko.lingko.core.domain.quota.exception.QuotaExceededException;
import com.lingko.lingko.core.domain.quota.repository.DailyPracticeQuotaRepository;
import com.lingko.lingko.core.domain.quota.repository.AdRewardReceiptRepository;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Practice 할당량 서비스 Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem=practice_quota;MODE=MySQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@Import({PracticeQuotaService.class, PracticeQuotaServiceTest.TestClockConfig.class})
class PracticeQuotaServiceTest {

    @Autowired
    private PracticeQuotaService quotaService;

    @Autowired
    private DailyPracticeQuotaRepository quotaRepository;

    @Autowired
    private AdRewardReceiptRepository adRewardReceiptRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.reset();
    }

    @Test
    @DisplayName("오늘 quota가 없으면 Asia/Seoul 날짜 기준으로 기본 5회 quota를 만든다")
    void createsDefaultTodayQuota() {
        User user = saveUser();

        PracticeQuotaResponse response = quotaService.getTodayQuota(user.getUserIdx());

        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 6, 29));
        assertThat(response.freeLimit()).isEqualTo(5);
        assertThat(response.freeUsed()).isZero();
        assertThat(response.rewardedAvailable()).isZero();
        assertThat(response.remainingPractices()).isEqualTo(5);
        assertThat(response.nextRefillAt()).isNull();
        assertThat(response.serverTime().toInstant()).isEqualTo(clock.instant());
        assertThat(quotaRepository.findByUserUserIdxAndQuotaDate(user.getUserIdx(), LocalDate.of(2026, 6, 29)))
                .isPresent();
    }

    @Test
    @DisplayName("평가 성공 차감은 무료 quota를 먼저 사용한다")
    void consumeFreeQuotaFirst() {
        User user = saveUser();

        PracticeQuotaResponse response = quotaService.consumePractice(user.getUserIdx());

        assertThat(response.freeUsed()).isEqualTo(1);
        assertThat(response.rewardedAvailable()).isZero();
        assertThat(response.remainingPractices()).isEqualTo(4);
        assertThat(response.nextRefillAt().toInstant())
                .isEqualTo(clock.instant().plus(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("한 시간마다 1회 충전되고 최대 5회에 도달하면 timer를 제거한다")
    void replenishesOnePracticeEveryHourUpToMaximum() {
        User user = saveUser();
        quotaService.consumePractice(user.getUserIdx());
        quotaService.consumePractice(user.getUserIdx());

        Instant firstRefillAt = clock.instant().plus(Duration.ofHours(1));
        assertThat(quotaService.getTodayQuota(user.getUserIdx()).nextRefillAt().toInstant())
                .isEqualTo(firstRefillAt);

        clock.advance(Duration.ofHours(1).plusMinutes(30));
        PracticeQuotaResponse afterOneInterval = quotaService.getTodayQuota(user.getUserIdx());
        assertThat(afterOneInterval.remainingPractices()).isEqualTo(4);
        assertThat(afterOneInterval.nextRefillAt().toInstant())
                .isEqualTo(firstRefillAt.plus(Duration.ofHours(1)));

        clock.advance(Duration.ofMinutes(30));
        PracticeQuotaResponse full = quotaService.getTodayQuota(user.getUserIdx());
        assertThat(full.remainingPractices()).isEqualTo(5);
        assertThat(full.nextRefillAt()).isNull();
    }

    @Test
    @DisplayName("추가 소모는 이미 진행 중인 자연 충전 timer를 초기화하지 않는다")
    void additionalConsumptionKeepsExistingRefillTimer() {
        User user = saveUser();
        quotaService.consumePractice(user.getUserIdx());
        Instant firstRefillAt = quotaService.getTodayQuota(user.getUserIdx())
                .nextRefillAt()
                .toInstant();

        clock.advance(Duration.ofMinutes(20));
        PracticeQuotaResponse response = quotaService.consumePractice(user.getUserIdx());

        assertThat(response.remainingPractices()).isEqualTo(3);
        assertThat(response.nextRefillAt().toInstant()).isEqualTo(firstRefillAt);
    }

    @Test
    @DisplayName("날짜가 바뀌어도 자정 초기화 없이 1시간 충전 주기를 유지한다")
    void keepsHourlyRefillAcrossMidnight() {
        User user = saveUser();
        clock.advance(Duration.ofHours(23).plusMinutes(20));
        for (int attempt = 0; attempt < 5; attempt++) {
            quotaService.consumePractice(user.getUserIdx());
        }

        Instant nextRefillAt = clock.instant().plus(Duration.ofHours(1));
        clock.advance(Duration.ofMinutes(20));
        PracticeQuotaResponse response = quotaService.getTodayQuota(user.getUserIdx());

        assertThat(response.remainingPractices()).isZero();
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(response.nextRefillAt().toInstant()).isEqualTo(nextRefillAt);
        assertThat(quotaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("무료 quota를 모두 쓰면 보상 quota를 사용한다")
    void consumeRewardQuotaAfterFreeQuota() {
        User user = saveUser();
        DailyPracticeQuota quota = DailyPracticeQuota.create(user, LocalDate.of(2026, 6, 29), 5);
        quota.useFreePractices(5);
        quota.addRewardedPractices(2);
        quotaRepository.save(quota);

        PracticeQuotaResponse response = quotaService.consumePractice(user.getUserIdx());

        assertThat(response.freeUsed()).isEqualTo(5);
        assertThat(response.rewardedAvailable()).isEqualTo(1);
        assertThat(response.remainingPractices()).isEqualTo(1);
    }

    @Test
    @DisplayName("남은 quota가 없으면 차감할 수 없다")
    void cannotConsumeWhenQuotaIsExhausted() {
        User user = saveUser();
        DailyPracticeQuota quota = DailyPracticeQuota.create(user, LocalDate.of(2026, 6, 29), 5);
        quota.useFreePractices(5);
        quotaRepository.save(quota);

        assertThatThrownBy(() -> quotaService.consumePractice(user.getUserIdx()))
                .isInstanceOf(QuotaExceededException.class);
    }

    @Test
    @DisplayName("무료 quota 예약을 취소하면 동일 날짜의 무료 사용량이 복구된다")
    void releasesReservedFreeQuota() {
        User user = saveUser();

        PracticeQuotaService.PracticeQuotaReservation reservation =
                quotaService.reservePractice(user.getUserIdx());
        PracticeQuotaResponse reserved = quotaService.getTodayQuota(user.getUserIdx());
        assertThat(reserved.remainingPractices()).isEqualTo(4);
        assertThat(reserved.nextRefillAt().toInstant())
                .isEqualTo(clock.instant().plus(Duration.ofHours(1)));
        quotaService.releasePractice(reservation);

        PracticeQuotaResponse response = quotaService.getTodayQuota(user.getUserIdx());
        assertThat(reservation.source()).isEqualTo(PracticeQuotaService.QuotaSource.FREE);
        assertThat(response.freeUsed()).isZero();
        assertThat(response.remainingPractices()).isEqualTo(5);
        assertThat(response.nextRefillAt()).isNull();
    }

    @Test
    @DisplayName("이미 복구된 quota 예약은 최종 실패 처리가 반복돼도 false를 반환한다")
    void reportsAlreadyReleasedReservationWithoutThrowing() {
        User user = saveUser();
        PracticeQuotaService.PracticeQuotaReservation reservation =
                quotaService.reservePractice(user.getUserIdx());
        quotaService.releasePractice(reservation);

        assertThat(quotaService.releasePracticeIfReserved(reservation)).isFalse();
        assertThat(quotaService.getTodayQuota(user.getUserIdx()).remainingPractices())
                .isEqualTo(5);
    }

    @Test
    @DisplayName("무료 quota 예약을 확정하면 예약량이 사용량으로 전환된다")
    void confirmsReservedFreeQuota() {
        User user = saveUser();

        PracticeQuotaService.PracticeQuotaReservation reservation =
                quotaService.reservePractice(user.getUserIdx());
        quotaService.confirmPractice(reservation);

        PracticeQuotaResponse response = quotaService.getTodayQuota(user.getUserIdx());
        assertThat(response.freeUsed()).isEqualTo(1);
        assertThat(response.remainingPractices()).isEqualTo(4);
    }

    @Test
    @DisplayName("보상 quota 예약을 취소하면 보상 횟수가 복구된다")
    void releasesReservedRewardedQuota() {
        User user = saveUser();
        DailyPracticeQuota quota = DailyPracticeQuota.create(user, LocalDate.of(2026, 6, 29), 5);
        quota.useFreePractices(5);
        quota.addRewardedPractices(1);
        quotaRepository.save(quota);

        PracticeQuotaService.PracticeQuotaReservation reservation =
                quotaService.reservePractice(user.getUserIdx());
        quotaService.releasePractice(reservation);

        PracticeQuotaResponse response = quotaService.getTodayQuota(user.getUserIdx());
        assertThat(reservation.source()).isEqualTo(PracticeQuotaService.QuotaSource.REWARDED);
        assertThat(response.rewardedAvailable()).isEqualTo(1);
        assertThat(response.remainingPractices()).isEqualTo(1);
    }

    @Test
    @DisplayName("광고 보상은 1회를 추가하되 진행 중인 자연 충전 timer를 바꾸지 않는다")
    void grantsOneAdRewardWithoutResettingRefillTimer() {
        User user = saveUser();
        quotaService.consumePractice(user.getUserIdx());
        quotaService.consumePractice(user.getUserIdx());
        Instant originalRefillAt = quotaService.getTodayQuota(user.getUserIdx())
                .nextRefillAt()
                .toInstant();

        clock.advance(Duration.ofMinutes(20));
        PracticeQuotaResponse response = quotaService.grantAdReward(user.getUserIdx(), "reward-event-123");

        assertThat(response.rewardedAvailable()).isEqualTo(1);
        assertThat(response.remainingPractices()).isEqualTo(4);
        assertThat(response.nextRefillAt().toInstant()).isEqualTo(originalRefillAt);
        assertThat(adRewardReceiptRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 광고 reward event는 재요청해도 한 번만 지급한다")
    void grantsSameRewardEventOnlyOnce() {
        User user = saveUser();
        quotaService.consumePractice(user.getUserIdx());
        quotaService.consumePractice(user.getUserIdx());

        quotaService.grantAdReward(user.getUserIdx(), "reward-event-123");
        PracticeQuotaResponse repeated = quotaService.grantAdReward(user.getUserIdx(), "reward-event-123");

        assertThat(repeated.rewardedAvailable()).isEqualTo(1);
        assertThat(repeated.remainingPractices()).isEqualTo(4);
        assertThat(adRewardReceiptRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("현재 기회가 최대 5회이면 광고 event를 기록하되 추가 지급하지 않는다")
    void doesNotExceedFivePractices() {
        User user = saveUser();

        PracticeQuotaResponse response = quotaService.grantAdReward(user.getUserIdx(), "reward-event-at-max");

        assertThat(response.remainingPractices()).isEqualTo(5);
        assertThat(response.rewardedAvailable()).isZero();
        assertThat(adRewardReceiptRepository.count()).isEqualTo(1);
    }

    private User saveUser() {
        return userRepository.save(User.builder()
                .socialId("google-sub-123")
                .socialType(User.SocialType.GOOGLE)
                .email("user@example.com")
                .name("LingKo User")
                .build());
    }

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        MutableClock clock() {
            return new MutableClock();
        }
    }

    static final class MutableClock extends Clock {
        private static final Instant INITIAL_INSTANT = Instant.parse("2026-06-28T15:30:00Z");
        private final ZoneId zone = ZoneId.of("Asia/Seoul");
        private Instant current = INITIAL_INSTANT;

        void reset() {
            current = INITIAL_INSTANT;
        }

        void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId requestedZone) {
            return Clock.fixed(current, requestedZone);
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
