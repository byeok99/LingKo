package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationJobRepository;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationLogRepository;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationAudioStorage;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobCreationService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobExecutor;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobProcessingService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobWorker;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import com.lingko.lingko.core.domain.quota.entity.DailyPracticeQuota;
import com.lingko.lingko.core.domain.quota.repository.DailyPracticeQuotaRepository;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * API와 분리된 단일 DB polling Worker가 작업을 유실·중복 없이 순차 처리하는지 검증한다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:independent_evaluation_worker;MODE=MySQL;DATABASE_TO_UPPER=false",
        "evaluation.worker.enabled=false",
        "evaluation.cleanup.enabled=false"
})
class IndependentEvaluationWorkerIntegrationTest {

    private static final int USER_COUNT = 8;
    private static final int JOBS_PER_USER = 5;
    private static final int JOB_COUNT = USER_COUNT * JOBS_PER_USER;
    @Autowired
    private EvaluationJobCreationService creationService;
    @Autowired
    private EvaluationJobProcessingService processingService;
    @Autowired
    private EvaluationJobRepository jobRepository;
    @Autowired
    private EvaluationLogRepository evaluationLogRepository;
    @Autowired
    private DailyPracticeQuotaRepository quotaRepository;
    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EvaluationAudioStorage audioStorage;
    @MockitoBean
    private EvaluationService evaluationService;

    @BeforeEach
    void setUp() {
        clearDatabase();
        when(audioStorage.download(anyString())).thenAnswer(invocation ->
                Path.of("/tmp", Math.abs(invocation.<String>getArgument(0).hashCode()) + ".wav")
        );
        when(evaluationService.evaluatePronunciation(any(Path.class), anyString(), any(Runnable.class)))
                .thenAnswer(invocation -> {
                    // 실제 service처럼 Azure 분석 뒤 callback을 실행해 phase 전이까지 통합 검증한다.
                    invocation.<Runnable>getArgument(2).run();
                    return PracticeResultResponse.builder()
                            .overallScore(90)
                            .characters(List.of())
                            .weakCharacters(List.of())
                            .build();
                });
    }

    @AfterEach
    void tearDown() {
        clearDatabase();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("독립 DB Worker 1개가 평가 40건을 중복 없이 완료하고 quota를 정확히 확정한다")
    void processesJobsWithSingleIndependentWorker() {
        List<String> jobIds = createJobs();
        EvaluationJobWorker worker = createWorker();

        for (int index = 0; index < JOB_COUNT; index++) {
            assertThat(worker.processNext()).isTrue();
        }
        assertThat(worker.processNext()).isFalse();

        List<EvaluationJob> savedJobs = jobRepository.findAll();
        assertThat(savedJobs).hasSize(JOB_COUNT);
        assertThat(savedJobs)
                .extracting(EvaluationJob::getStatus)
                .containsOnly(EvaluationJob.Status.SUCCEEDED);
        assertThat(evaluationLogRepository.count()).isEqualTo(JOB_COUNT);
        assertThat(savedJobs)
                .extracting(EvaluationJob::getJobId)
                .containsExactlyInAnyOrderElementsOf(jobIds);

        List<DailyPracticeQuota> quotas = quotaRepository.findAll();
        assertThat(quotas).hasSize(USER_COUNT);
        assertThat(quotas).allSatisfy(quota -> {
            assertThat(quota.getFreeUsed()).isEqualTo(JOBS_PER_USER);
            assertThat(quota.getFreeReserved()).isZero();
        });
    }

    private List<String> createJobs() {
        List<String> jobIds = new ArrayList<>();
        for (int userIndex = 0; userIndex < USER_COUNT; userIndex++) {
            for (int jobIndex = 0; jobIndex < JOBS_PER_USER; jobIndex++) {
                jobIds.add(createJob(userIndex, jobIndex));
            }
        }
        return jobIds;
    }

    private String createJob(int userIndex, int jobIndex) {
        EvaluationJobService.EvaluationTarget target = new EvaluationJobService.EvaluationTarget(
                EvaluationLog.PracticeSource.CUSTOM,
                null,
                "안녕하세요.",
                "안녕하세여."
        );
        User user = userRepository.findBySocialIdAndSocialType(
                        "worker-load-user-" + userIndex,
                        User.SocialType.GOOGLE
                )
                .orElseGet(() -> userRepository.saveAndFlush(User.builder()
                        .socialId("worker-load-user-" + userIndex)
                        .socialType(User.SocialType.GOOGLE)
                        .build()));
        EvaluationJob job = creationService.create(
                user.getUserIdx(),
                "worker-key-" + userIndex + "-" + jobIndex,
                "worker-hash-" + userIndex + "-" + jobIndex,
                "evaluation-audio/" + user.getUserIdx() + "/" + jobIndex + ".wav",
                target
        );
        return job.getJobId();
    }

    private EvaluationJobWorker createWorker() {
        EvaluationJobExecutor executor = new EvaluationJobExecutor(
                processingService,
                audioStorage,
                evaluationService
        );
        return new EvaluationJobWorker(processingService, executor);
    }

    private void clearDatabase() {
        evaluationLogRepository.deleteAll();
        jobRepository.deleteAll();
        quotaRepository.deleteAll();
        userRepository.deleteAll();
    }

}
