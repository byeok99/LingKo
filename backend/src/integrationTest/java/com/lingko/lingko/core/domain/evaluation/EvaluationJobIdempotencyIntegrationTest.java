package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.api.evaluation.dto.EvaluationJobRequest;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationJobRepository;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationAudioStorage;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobCleanupService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import com.lingko.lingko.core.domain.quota.entity.DailyPracticeQuota;
import com.lingko.lingko.core.domain.quota.repository.DailyPracticeQuotaRepository;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 실제 Spring transaction과 JPA에서 평가 Idempotency의 동시 생성과 보존 만료 계약을 검증한다.
 */
@SpringBootTest(properties = {
        "evaluation.worker.enabled=false",
        "evaluation.cleanup.enabled=false"
})
@Import(EvaluationJobIdempotencyIntegrationTest.TestClockConfig.class)
class EvaluationJobIdempotencyIntegrationTest {

    private static final int CONCURRENT_REQUESTS = 10;
    private static final Instant NOW = Instant.parse("2026-07-29T03:00:00Z");
    private static final LocalDate QUOTA_DATE = LocalDate.of(2026, 7, 29);
    private static final String MYSQL_TEST_URL = System.getenv("IDEMPOTENCY_TEST_DB_URL");

    @Autowired
    private EvaluationJobService jobService;
    @Autowired
    private EvaluationJobCleanupService cleanupService;
    @Autowired
    private EvaluationJobRepository jobRepository;
    @Autowired
    private DailyPracticeQuotaRepository quotaRepository;
    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EvaluationAudioStorage audioStorage;
    @MockitoBean
    private EvaluationService evaluationService;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        boolean mysqlTest = MYSQL_TEST_URL != null && !MYSQL_TEST_URL.isBlank();
        registry.add(
                "spring.datasource.url",
                () -> mysqlTest
                        ? MYSQL_TEST_URL
                        : "jdbc:h2:mem:evaluation_job_idempotency;MODE=MySQL;DATABASE_TO_UPPER=false"
        );
        registry.add(
                "spring.datasource.username",
                () -> environment("IDEMPOTENCY_TEST_DB_USER", "sa")
        );
        registry.add(
                "spring.datasource.password",
                () -> environment("IDEMPOTENCY_TEST_DB_PASSWORD", "")
        );
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
        clearDatabase();
    }

    @AfterEach
    void clearAfterTest() {
        clearDatabase();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("동일 사용자의 같은 Idempotency 요청 10개는 작업과 quota 예약을 한 번만 생성한다")
    void createsOneJobAndOneQuotaReservationForConcurrentDuplicates() throws Exception {
        User user = saveUser("idempotency-concurrent-user");
        EvaluationJobRequest request = new EvaluationJobRequest(
                "evaluation-audio/" + user.getUserIdx() + "/recording.wav",
                null,
                "안녕하세요."
        );
        when(evaluationService.convertToStandardPronunciation(anyString()))
                .thenReturn("안녕하세여.");

        List<String> jobIds = runConcurrently(() -> jobService.createJob(
                user.getUserIdx(),
                "same-idempotency-key",
                request
        ).jobId());

        assertThat(jobIds).hasSize(CONCURRENT_REQUESTS).containsOnly(jobIds.getFirst());
        assertThat(jobRepository.count()).isEqualTo(1);

        DailyPracticeQuota quota = quotaRepository
                .findByUserUserIdxAndQuotaDate(user.getUserIdx(), QUOTA_DATE)
                .orElseThrow();
        assertThat(quota.getFreeReserved()).isEqualTo(1);
        assertThat(quota.getFreeUsed()).isZero();
        assertThat(quota.remainingPractices()).isEqualTo(4);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("정리 작업은 보존 기간이 지난 완료 작업만 삭제하고 진행 중 작업은 유지한다")
    void deletesOnlyExpiredTerminalJobs() {
        User user = saveUser("idempotency-cleanup-user");
        EvaluationJob expiredSuccess = saveJob(user, "expired-success", "expired-success.wav");
        expiredSuccess.succeed("{}", NOW.minus(Duration.ofDays(8)));
        EvaluationJob expiredFailure = saveJob(user, "expired-failure", "expired-failure.wav");
        expiredFailure.fail("EVALUATION_FAILED", NOW.minus(Duration.ofDays(8)));
        EvaluationJob recentSuccess = saveJob(user, "recent-success", "recent-success.wav");
        recentSuccess.succeed("{}", NOW.minus(Duration.ofDays(6)));
        EvaluationJob pending = saveJob(user, "old-pending", "old-pending.wav");
        jobRepository.saveAllAndFlush(List.of(
                expiredSuccess,
                expiredFailure,
                recentSuccess,
                pending
        ));

        int deleted = cleanupService.cleanupExpired();

        assertThat(deleted).isEqualTo(2);
        assertThat(jobRepository.findById(expiredSuccess.getJobId())).isEmpty();
        assertThat(jobRepository.findById(expiredFailure.getJobId())).isEmpty();
        assertThat(jobRepository.findById(recentSuccess.getJobId())).isPresent();
        assertThat(jobRepository.findById(pending.getJobId())).isPresent();
    }

    private EvaluationJob saveJob(User user, String suffix, String objectName) {
        return EvaluationJob.create(
                "job-" + suffix,
                user,
                "key-" + suffix,
                "hash-" + suffix,
                "evaluation-audio/" + user.getUserIdx() + "/" + objectName,
                EvaluationLog.PracticeSource.CUSTOM,
                null,
                "안녕하세요.",
                "안녕하세여.",
                QUOTA_DATE,
                PracticeQuotaService.QuotaSource.FREE,
                NOW.minus(Duration.ofDays(60))
        );
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

    private void clearDatabase() {
        jobRepository.deleteAll();
        quotaRepository.deleteAll();
        userRepository.deleteAll();
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

    @TestConfiguration(proxyBeanMethods = false)
    static class TestClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
