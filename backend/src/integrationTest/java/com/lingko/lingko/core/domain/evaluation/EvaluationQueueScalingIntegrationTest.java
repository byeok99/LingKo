package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.config.EvaluationJobSettings;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationJobRepository;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationLogRepository;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationAudioStorage;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobCreationService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobExecutor;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobProcessingService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobQueue;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobQueueWorker;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobService;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 여러 독립 Worker 인스턴스가 Queue와 DB lease를 공유해 작업을 정확히 한 번 완료하는지 검증한다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:evaluation_queue_scaling;MODE=MySQL;DATABASE_TO_UPPER=false",
        "evaluation.worker.enabled=false",
        "evaluation.cleanup.enabled=false"
})
class EvaluationQueueScalingIntegrationTest {

    private static final int USER_COUNT = 8;
    private static final int JOBS_PER_USER = 5;
    private static final int JOB_COUNT = USER_COUNT * JOBS_PER_USER;
    private static final int WORKER_COUNT = 4;

    @Autowired
    private EvaluationJobCreationService creationService;
    @Autowired
    private EvaluationJobProcessingService processingService;
    @Autowired
    private EvaluationJobSettings settings;
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
        when(evaluationService.evaluatePronunciation(any(Path.class), anyString()))
                .thenReturn(PracticeResultResponse.builder()
                        .overallScore(90)
                        .characters(List.of())
                        .weakCharacters(List.of())
                        .build());
    }

    @AfterEach
    void tearDown() {
        clearDatabase();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Queue Worker 4개가 평가 40건을 중복 없이 완료하고 quota를 정확히 확정한다")
    void scalesWorkersWithoutDuplicateCompletion() throws Exception {
        InMemoryEvaluationJobQueue queue = new InMemoryEvaluationJobQueue();
        List<String> jobIds = createJobs();
        jobIds.forEach(queue::publish);
        List<EvaluationJobQueueWorker> workers = createWorkers(queue);

        ExecutorService executorService = Executors.newFixedThreadPool(WORKER_COUNT);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int index = 0; index < JOB_COUNT; index++) {
                EvaluationJobQueueWorker worker = workers.get(index % WORKER_COUNT);
                results.add(executorService.submit(worker::processNext));
            }
            for (Future<Boolean> result : results) {
                assertThat(result.get(20, TimeUnit.SECONDS)).isTrue();
            }
        } finally {
            executorService.shutdownNow();
            assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }

        List<EvaluationJob> savedJobs = jobRepository.findAll();
        assertThat(savedJobs).hasSize(JOB_COUNT);
        assertThat(savedJobs)
                .extracting(EvaluationJob::getStatus)
                .containsOnly(EvaluationJob.Status.SUCCEEDED);
        assertThat(evaluationLogRepository.count()).isEqualTo(JOB_COUNT);
        assertThat(queue.acknowledgedJobIds()).containsExactlyInAnyOrderElementsOf(jobIds);
        assertThat(queue.releaseCount()).isZero();

        List<DailyPracticeQuota> quotas = quotaRepository.findAll();
        assertThat(quotas).hasSize(USER_COUNT);
        assertThat(quotas).allSatisfy(quota -> {
            assertThat(quota.getFreeUsed()).isEqualTo(JOBS_PER_USER);
            assertThat(quota.getFreeReserved()).isZero();
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("동일 작업이 다시 전달돼도 평가 결과와 quota는 한 번만 확정한다")
    void handlesDuplicateDeliveryIdempotently() {
        InMemoryEvaluationJobQueue queue = new InMemoryEvaluationJobQueue();
        String jobId = createJob(0, 0);
        queue.publish(jobId);
        queue.publish(jobId);
        EvaluationJobQueueWorker worker = createWorkers(queue).getFirst();

        assertThat(worker.processNext()).isTrue();
        assertThat(worker.processNext()).isTrue();

        EvaluationJob savedJob = jobRepository.findById(jobId).orElseThrow();
        assertThat(savedJob.getStatus()).isEqualTo(EvaluationJob.Status.SUCCEEDED);
        assertThat(evaluationLogRepository.count()).isOne();
        assertThat(queue.acknowledgedJobIds()).containsExactly(jobId);
        assertThat(queue.releaseCount()).isZero();

        List<DailyPracticeQuota> quotas = quotaRepository.findAll();
        assertThat(quotas).singleElement().satisfies(quota -> {
            assertThat(quota.getFreeUsed()).isOne();
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
                        "queue-load-user-" + userIndex,
                        User.SocialType.GOOGLE
                )
                .orElseGet(() -> userRepository.saveAndFlush(User.builder()
                        .socialId("queue-load-user-" + userIndex)
                        .socialType(User.SocialType.GOOGLE)
                        .build()));
        EvaluationJob job = creationService.create(
                user.getUserIdx(),
                "queue-key-" + userIndex + "-" + jobIndex,
                "queue-hash-" + userIndex + "-" + jobIndex,
                "evaluation-audio/" + user.getUserIdx() + "/" + jobIndex + ".wav",
                target
        );
        return job.getJobId();
    }

    private List<EvaluationJobQueueWorker> createWorkers(EvaluationJobQueue queue) {
        List<EvaluationJobQueueWorker> workers = new ArrayList<>();
        EvaluationJobExecutor executor = new EvaluationJobExecutor(
                processingService,
                audioStorage,
                evaluationService
        );
        for (int index = 0; index < WORKER_COUNT; index++) {
            workers.add(new EvaluationJobQueueWorker(
                    queue,
                    processingService,
                    executor,
                    settings
            ));
        }
        return workers;
    }

    private void clearDatabase() {
        evaluationLogRepository.deleteAll();
        jobRepository.deleteAll();
        quotaRepository.deleteAll();
        userRepository.deleteAll();
    }

    private static final class InMemoryEvaluationJobQueue implements EvaluationJobQueue {

        private final ConcurrentLinkedQueue<Message> messages =
                new ConcurrentLinkedQueue<>();
        private final Set<String> acknowledgedJobIds = ConcurrentHashMap.newKeySet();
        private final AtomicInteger receiptSequence = new AtomicInteger();
        private final AtomicInteger releaseCount = new AtomicInteger();

        @Override
        public void publish(String jobId) {
            messages.add(new Message(
                    jobId,
                    "receipt-" + receiptSequence.incrementAndGet()
            ));
        }

        @Override
        public Optional<Message> receive() {
            return Optional.ofNullable(messages.poll());
        }

        @Override
        public void acknowledge(Message message) {
            acknowledgedJobIds.add(message.jobId());
        }

        @Override
        public void release(Message message, int delaySeconds) {
            releaseCount.incrementAndGet();
            messages.add(message);
        }

        Set<String> acknowledgedJobIds() {
            return Set.copyOf(acknowledgedJobIds);
        }

        int releaseCount() {
            return releaseCount.get();
        }
    }
}
