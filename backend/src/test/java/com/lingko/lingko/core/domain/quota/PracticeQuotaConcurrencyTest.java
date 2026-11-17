package com.lingko.lingko.core.domain.quota;

import com.lingko.lingko.core.domain.quota.entity.DailyPracticeQuota;
import com.lingko.lingko.core.domain.quota.exception.QuotaExceededException;
import com.lingko.lingko.core.domain.quota.repository.DailyPracticeQuotaRepository;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동일 사용자 쿼터 요청이 동시에 실행되어도 초과 예약과 당일 행 중복이 발생하지 않는지 검증한다.
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PracticeQuotaService.class, PracticeQuotaConcurrencyTest.TestClockConfig.class})
class PracticeQuotaConcurrencyTest {

    private static final int CONCURRENT_REQUESTS = 10;
    private static final LocalDate QUOTA_DATE = LocalDate.of(2026, 6, 29);
    private static final String MYSQL_TEST_URL = System.getenv("QUOTA_TEST_DB_URL");

    @Autowired
    private PracticeQuotaService quotaService;

    @Autowired
    private DailyPracticeQuotaRepository quotaRepository;

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        boolean mysqlTest = MYSQL_TEST_URL != null && !MYSQL_TEST_URL.isBlank();
        registry.add(
                "spring.datasource.url",
                () -> mysqlTest
                        ? MYSQL_TEST_URL
                        : "jdbc:h2:mem:practice_quota_concurrency;MODE=MySQL;DATABASE_TO_UPPER=false"
        );
        registry.add("spring.datasource.username", () -> environment("QUOTA_TEST_DB_USER", "sa"));
        registry.add("spring.datasource.password", () -> environment("QUOTA_TEST_DB_PASSWORD", ""));
        registry.add(
                "spring.datasource.driver-class-name",
                () -> mysqlTest ? "com.mysql.cj.jdbc.Driver" : "org.h2.Driver"
        );
        registry.add(
                "spring.jpa.properties.hibernate.dialect",
                () -> mysqlTest
                        ? "org.hibernate.dialect.MySQLDialect"
                        : "org.hibernate.dialect.H2Dialect"
        );
    }

    @BeforeEach
    void cleanDatabase() {
        quotaRepository.deleteAll();
        userRepository.deleteAll();
    }

    @RepeatedTest(3)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("남은 quota 1회에 동시 예약 10개가 들어오면 정확히 1개만 성공한다")
    void allowsOnlyOneReservationWhenOnePracticeRemains() throws Exception {
        // Given: 남은 quota 1회
        User user = saveUser("reservation-user-" + System.nanoTime());
        DailyPracticeQuota quota = DailyPracticeQuota.builder()
                .user(user)
                .quotaDate(QUOTA_DATE)
                .freeLimit(5)
                .freeUsed(4)
                .rewardedAvailable(0)
                .freeReserved(0)
                .rewardedReserved(0)
                .build();
        quotaRepository.saveAndFlush(quota);

        // When
        List<ReservationOutcome> outcomes = runConcurrently(() -> {
            try {
                quotaService.reservePractice(user.getUserIdx());
                return ReservationOutcome.SUCCESS;
            } catch (QuotaExceededException exception) {
                return ReservationOutcome.QUOTA_EXCEEDED;
            }
        });

        // Then
        assertThat(outcomes).filteredOn(ReservationOutcome.SUCCESS::equals).hasSize(1);
        assertThat(outcomes).filteredOn(ReservationOutcome.QUOTA_EXCEEDED::equals).hasSize(9);

        DailyPracticeQuota saved = quotaRepository
                .findByUserUserIdxAndQuotaDate(user.getUserIdx(), QUOTA_DATE)
                .orElseThrow();
        assertThat(saved.getFreeReserved()).isEqualTo(1);
        assertThat(saved.remainingPractices()).isZero();
    }

    @RepeatedTest(3)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("신규 사용자의 오늘 quota 동시 조회는 사용자·날짜별 행을 하나만 만든다")
    void createsOnlyOneDailyQuotaRow() throws Exception {
        // Given
        User user = saveUser("creation-user-" + System.nanoTime());

        // When
        List<Integer> remainingCounts = runConcurrently(
                () -> quotaService.getTodayQuota(user.getUserIdx()).remainingPractices()
        );

        // Then
        assertThat(remainingCounts).containsOnly(5).hasSize(CONCURRENT_REQUESTS);
        assertThat(quotaRepository.count()).isEqualTo(1);
        assertThat(quotaRepository.findByUserUserIdxAndQuotaDate(user.getUserIdx(), QUOTA_DATE))
                .isPresent();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("동일 예약을 동시에 확정하거나 복구해도 상태 전이는 한 번만 반영된다")
    void appliesEachReservationTransitionOnlyOnce() throws Exception {
        // Given
        User user = saveUser("transition-user-" + System.nanoTime());

        // When: 같은 예약을 동시에 확정한다
        PracticeQuotaService.PracticeQuotaReservation confirmedReservation =
                quotaService.reservePractice(user.getUserIdx());
        List<TransitionOutcome> confirmOutcomes = runConcurrently(() -> {
            try {
                quotaService.confirmPractice(confirmedReservation);
                return TransitionOutcome.SUCCESS;
            } catch (IllegalStateException exception) {
                return TransitionOutcome.ALREADY_APPLIED;
            }
        });

        // Then
        assertSingleTransition(confirmOutcomes);
        DailyPracticeQuota afterConfirm = quotaRepository
                .findByUserUserIdxAndQuotaDate(user.getUserIdx(), QUOTA_DATE)
                .orElseThrow();
        assertThat(afterConfirm.getFreeUsed()).isEqualTo(1);
        assertThat(afterConfirm.getFreeReserved()).isZero();

        // When: 같은 예약을 동시에 복구한다
        PracticeQuotaService.PracticeQuotaReservation releasedReservation =
                quotaService.reservePractice(user.getUserIdx());
        List<TransitionOutcome> releaseOutcomes = runConcurrently(() -> {
            try {
                quotaService.releasePractice(releasedReservation);
                return TransitionOutcome.SUCCESS;
            } catch (IllegalStateException exception) {
                return TransitionOutcome.ALREADY_APPLIED;
            }
        });

        // Then
        assertSingleTransition(releaseOutcomes);
        DailyPracticeQuota afterRelease = quotaRepository
                .findByUserUserIdxAndQuotaDate(user.getUserIdx(), QUOTA_DATE)
                .orElseThrow();
        assertThat(afterRelease.getFreeUsed()).isEqualTo(1);
        assertThat(afterRelease.getFreeReserved()).isZero();
        assertThat(afterRelease.remainingPractices()).isEqualTo(4);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("무료 예약 둘 중 하나만 복구하면 충전 timer를 유지하고 마지막 복구에서만 지운다")
    void keepsRefillTimerUntilLastFreeReservationIsReleased() {
        // Given: 확정 사용량 없이 무료 예약 2개가 진행 중이다
        User user = saveUser("release-timer-user-" + System.nanoTime());
        PracticeQuotaService.PracticeQuotaReservation first = quotaService.reservePractice(user.getUserIdx());
        PracticeQuotaService.PracticeQuotaReservation second = quotaService.reservePractice(user.getUserIdx());

        // When: 하나만 복구한다
        quotaService.releasePractice(first);

        // Then: 남은 예약이 있으므로 timer가 유지된다 (MySQL SET 평가 순서 회귀)
        assertThat(currentQuota(user).getNextRefillAt()).isNotNull();

        // When: 마지막 예약도 복구한다
        quotaService.releasePractice(second);

        // Then
        assertThat(currentQuota(user).getNextRefillAt()).isNull();
    }

    private DailyPracticeQuota currentQuota(User user) {
        return quotaRepository.findByUserUserIdxAndQuotaDate(user.getUserIdx(), QUOTA_DATE).orElseThrow();
    }

    private User saveUser(String socialId) {
        return userRepository.saveAndFlush(User.builder()
                .socialId(socialId)
                .socialType(User.SocialType.GOOGLE)
                .build());
    }

    private static String environment(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null ? defaultValue : value;
    }

    private <T> List<T> runConcurrently(Callable<T> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int request = 0; request < CONCURRENT_REQUESTS; request++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("concurrent test start timed out");
                    }
                    return task.call();
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void assertSingleTransition(List<TransitionOutcome> outcomes) {
        assertThat(outcomes).filteredOn(TransitionOutcome.SUCCESS::equals).hasSize(1);
        assertThat(outcomes).filteredOn(TransitionOutcome.ALREADY_APPLIED::equals).hasSize(9);
    }

    private enum ReservationOutcome {
        SUCCESS,
        QUOTA_EXCEEDED
    }

    private enum TransitionOutcome {
        SUCCESS,
        ALREADY_APPLIED
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
