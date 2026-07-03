package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.api.evaluation.dto.GuideGenerationJobResponse;
import com.lingko.lingko.core.domain.evaluation.dto.GuideGenerationJobStatus;
import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
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

@Service
@Slf4j
public class GuideGenerationJobService {
    private static final String FAILURE_MESSAGE = "Guide generation failed";

    private final VideoGenerator videoGenerator;
    private final Executor executor;
    private final ConcurrentHashMap<String, GuideGenerationJobResponse> jobsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> jobIdByCacheKey = new ConcurrentHashMap<>();

    @Autowired
    public GuideGenerationJobService(VideoGenerator videoGenerator) {
        this(videoGenerator, ForkJoinPool.commonPool());
    }

    public GuideGenerationJobService(VideoGenerator videoGenerator, Executor executor) {
        this.videoGenerator = videoGenerator;
        this.executor = executor;
    }

    public synchronized GuideGenerationJobResponse submit(String syllable, VideoType type, List<List<String>> urlPairs) {
        validate(syllable, type, urlPairs);

        List<List<String>> normalizedPairs = normalize(urlPairs);
        String trimmedSyllable = syllable.trim();
        String cacheKey = cacheKey(trimmedSyllable, type, normalizedPairs);
        String existingJobId = jobIdByCacheKey.get(cacheKey);
        if (existingJobId != null) {
            return jobsById.get(existingJobId);
        }

        String jobId = UUID.randomUUID().toString();
        GuideGenerationJobResponse pending = response(jobId, GuideGenerationJobStatus.PENDING, cacheKey, null, null);
        jobsById.put(jobId, pending);
        jobIdByCacheKey.put(cacheKey, jobId);

        executor.execute(() -> process(jobId, trimmedSyllable, type, normalizedPairs, cacheKey));
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
        } catch (Exception exception) {
            log.warn("Guide generation job failed: {}", jobId, exception);
            jobsById.put(jobId, response(jobId, GuideGenerationJobStatus.FAILED, cacheKey, null, FAILURE_MESSAGE));
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
