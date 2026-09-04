package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.api.evaluation.dto.GuideGenerationJobResponse;
import com.lingko.lingko.core.config.GuideGenerationJobSettings;
import com.lingko.lingko.core.domain.evaluation.dto.GuideGenerationJobStatus;
import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.core.domain.evaluation.exception.GuideJobCapacityExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Guide Generation Job 업무 규칙을 조율한다.
 *
 * 컨트롤러와 외부 어댑터가 정책을 소유하지 않도록 도메인 서비스에 조율을 집중했다.
 */
@Service
@Slf4j
public class GuideGenerationJobService {
    private static final String FAILURE_MESSAGE = "Guide generation failed";

    private final VideoGenerator videoGenerator;
    private final Executor executor;
    private final GuideSourceUrlPolicy sourceUrlPolicy;
    private final int maxConcurrent;
    private final GuideGenerationJobTelemetry telemetry;
    private final AtomicInteger activeJobs = new AtomicInteger();
    // 현재 단일 instance prototype에는 memory 상태를 사용하며 durable job queue가 아님을 명시한다.
    private final ConcurrentHashMap<String, GuideGenerationJobResponse> jobsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> jobIdByCacheKey = new ConcurrentHashMap<>();

    @Autowired
    public GuideGenerationJobService(
            VideoGenerator videoGenerator,
            GuideSourceUrlPolicy sourceUrlPolicy,
            GuideGenerationJobSettings settings,
            GuideGenerationJobTelemetry telemetry
    ) {
        this(
                videoGenerator,
                ForkJoinPool.commonPool(),
                sourceUrlPolicy,
                settings.getMaxConcurrent(),
                telemetry
        );
    }

    public GuideGenerationJobService(VideoGenerator videoGenerator, Executor executor) {
        this(videoGenerator, executor, ignored -> { }, 1, GuideGenerationJobTelemetry.NOOP);
    }

    public GuideGenerationJobService(
            VideoGenerator videoGenerator,
            Executor executor,
            GuideSourceUrlPolicy sourceUrlPolicy,
            int maxConcurrent
    ) {
        this(videoGenerator, executor, sourceUrlPolicy, maxConcurrent, GuideGenerationJobTelemetry.NOOP);
    }

    private GuideGenerationJobService(
            VideoGenerator videoGenerator,
            Executor executor,
            GuideSourceUrlPolicy sourceUrlPolicy,
            int maxConcurrent,
            GuideGenerationJobTelemetry telemetry
    ) {
        this.videoGenerator = videoGenerator;
        this.executor = executor;
        this.sourceUrlPolicy = sourceUrlPolicy;
        this.maxConcurrent = maxConcurrent;
        this.telemetry = telemetry;
    }

    public synchronized GuideGenerationJobResponse submit(String syllable, VideoType type, List<List<String>> urlPairs) {
        List<List<String>> normalizedPairs;
        try {
            validate(syllable, type, urlPairs);
            normalizedPairs = normalize(urlPairs);
            normalizedPairs.stream().flatMap(List::stream).forEach(sourceUrlPolicy::validate);
        } catch (IllegalArgumentException exception) {
            telemetry.request("invalid");
            throw exception;
        }
        String trimmedSyllable = syllable.trim();
        String cacheKey = cacheKey(trimmedSyllable, type, normalizedPairs);
        // cache 조회와 job 등록을 하나의 원자적 중복 제거 결정으로 만들기 위해 동기화한다.
        String existingJobId = jobIdByCacheKey.get(cacheKey);
        if (existingJobId != null) {
            telemetry.request("deduplicated");
            return jobsById.get(existingJobId);
        }

        if (activeJobs.get() >= maxConcurrent) {
            telemetry.request("capacity_limited");
            throw new GuideJobCapacityExceededException();
        }

        String jobId = UUID.randomUUID().toString();
        GuideGenerationJobResponse pending = response(jobId, GuideGenerationJobStatus.PENDING, cacheKey, null, null);
        jobsById.put(jobId, pending);
        jobIdByCacheKey.put(cacheKey, jobId);
        activeJobs.incrementAndGet();
        telemetry.request("accepted");
        telemetry.jobStarted();
        log.info(
                "Guide job accepted: jobId={}, syllable={}, type={}, sourceCount={}",
                jobId,
                trimmedSyllable,
                type,
                normalizedPairs.stream().mapToInt(List::size).sum()
        );

        try {
            executor.execute(() -> process(jobId, trimmedSyllable, type, normalizedPairs, cacheKey));
        } catch (RuntimeException exception) {
            jobsById.remove(jobId);
            jobIdByCacheKey.remove(cacheKey, jobId);
            activeJobs.decrementAndGet();
            telemetry.jobFinished("rejected");
            throw exception;
        }
        return jobsById.get(jobId);
    }

    public Optional<GuideGenerationJobResponse> find(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(jobsById.get(jobId));
    }

    private void process(String jobId, String syllable, VideoType type, List<List<String>> urlPairs, String cacheKey) {
        jobsById.put(jobId, response(jobId, GuideGenerationJobStatus.PROCESSING, cacheKey, null, null));

        try {
            String resultUrl = videoGenerator.generate(urlPairs, syllable, type);
            jobsById.put(jobId, response(jobId, GuideGenerationJobStatus.COMPLETED, cacheKey, resultUrl, null));
            telemetry.jobFinished("completed");
        } catch (Exception exception) {
            log.warn("Guide generation job failed: {}", jobId, exception);
            jobsById.put(jobId, response(jobId, GuideGenerationJobStatus.FAILED, cacheKey, null, FAILURE_MESSAGE));
            telemetry.jobFinished("failed");
        } finally {
            activeJobs.decrementAndGet();
        }
    }

    private void validate(String syllable, VideoType type, List<List<String>> urlPairs) {
        if (syllable == null || syllable.isBlank()) {
            throw new IllegalArgumentException("syllable is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (urlPairs == null || urlPairs.isEmpty()) {
            throw new IllegalArgumentException("urlPairs is required");
        }
    }

    private List<List<String>> normalize(List<List<String>> urlPairs) {
        // 의미가 같은 요청이 동일한 cache key를 사용하도록 공백을 canonical 형태로 정규화한다.
        return urlPairs.stream()
                .map(pair -> pair.stream()
                        .map(String::trim)
                        .toList())
                .toList();
    }

    private String cacheKey(String syllable, VideoType type, List<List<String>> urlPairs) {
        StringBuilder builder = new StringBuilder()
                .append(type.name())
                .append('|')
                .append(syllable);

        for (List<String> pair : urlPairs) {
            builder.append('|').append(String.join(",", pair));
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private GuideGenerationJobResponse response(
            String jobId,
            GuideGenerationJobStatus status,
            String cacheKey,
            String resultUrl,
            String errorMessage
    ) {
        return GuideGenerationJobResponse.builder()
                .jobId(jobId)
                .status(status)
                .cacheKey(cacheKey)
                .resultUrl(resultUrl)
                .errorMessage(errorMessage)
                .build();
    }
}
