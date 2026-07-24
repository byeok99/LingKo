package com.lingko.lingko.core.domain.quota;

import com.lingko.lingko.api.quota.dto.PracticeQuotaResponse;
import com.lingko.lingko.core.domain.quota.entity.DailyPracticeQuota;
import com.lingko.lingko.core.domain.quota.exception.QuotaExceededException;
import com.lingko.lingko.core.domain.quota.repository.DailyPracticeQuotaRepository;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
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
    private UserRepository userRepository;

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
        assertThat(response.resetAt().toString()).isEqualTo("2026-06-30T00:00+09:00");
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
        Clock clock() {
            return Clock.fixed(
                    Instant.parse("2026-06-28T15:30:00Z"),
                    ZoneId.of("Asia/Seoul")
            );
        }
    }
}
