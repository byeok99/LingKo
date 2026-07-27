package com.lingko.lingko.core.domain.evaluation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.api.evaluation.dto.EvaluationJobRequest;
import com.lingko.lingko.api.evaluation.dto.EvaluationJobResponse;
import com.lingko.lingko.api.evaluation.dto.EvaluationUploadRequest;
import com.lingko.lingko.api.evaluation.dto.EvaluationUploadResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.exception.EvaluationJobConflictException;
import com.lingko.lingko.core.domain.evaluation.exception.EvaluationJobNotFoundException;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationJobRepository;
import com.lingko.lingko.core.domain.sentence.entity.RecommendedSentence;
import com.lingko.lingko.core.domain.sentence.exception.SentenceNotFoundException;
import com.lingko.lingko.core.domain.sentence.repository.RecommendedSentenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * 직접 업로드 발급, Idempotency 기반 작업 생성과 사용자 소유 상태 조회를 조율한다.
 */
@Service
@RequiredArgsConstructor
public class EvaluationJobService {

    private static final Pattern IDEMPOTENCY_KEY =
            Pattern.compile("[A-Za-z0-9._:-]{8,100}");

    private final EvaluationAudioStorage audioStorage;
    private final EvaluationJobRepository jobRepository;
    private final EvaluationJobCreationService creationService;
    private final RecommendedSentenceRepository sentenceRepository;
    private final EvaluationService evaluationService;
    private final ObjectMapper objectMapper;

    public EvaluationUploadResponse prepareUpload(Long userId, EvaluationUploadRequest request) {
        EvaluationAudioStorage.UploadTicket ticket = audioStorage.prepareUpload(
                userId,
                request.fileName(),
                request.contentType(),
                request.contentLength()
        );
        return new EvaluationUploadResponse(
                ticket.objectKey(),
                ticket.uploadUrl(),
                ticket.expiresAt()
        );
    }

    public EvaluationJobResponse createJob(
            Long userId,
            String idempotencyKey,
            EvaluationJobRequest request
    ) {
        validateIdempotencyKey(idempotencyKey);
        validateTarget(request);
        String requestHash = requestHash(request);

        EvaluationJob existing = jobRepository
                .findByUserUserIdxAndIdempotencyKey(userId, idempotencyKey)
                .orElse(null);
        if (existing != null) {
            if (!existing.hasSameRequestHash(requestHash)) {
                throw new EvaluationJobConflictException();
            }
            return toResponse(existing);
        }

        audioStorage.validateUploaded(userId, request.objectKey());
        EvaluationTarget target = resolveTarget(request);
        return toResponse(creationService.create(
                userId,
                idempotencyKey,
                requestHash,
                request.objectKey(),
                target
        ));
    }

    public EvaluationJobResponse getJob(Long userId, String jobId) {
        return jobRepository.findByJobIdAndUserUserIdx(jobId, userId)
                .map(this::toResponse)
                .orElseThrow(EvaluationJobNotFoundException::new);
    }

    private EvaluationTarget resolveTarget(EvaluationJobRequest request) {
        if (request.sentenceId() != null) {
            RecommendedSentence sentence = sentenceRepository
                    .findBySentenceIdAndActiveTrue(request.sentenceId())
                    .orElseThrow(() -> new SentenceNotFoundException(request.sentenceId()));
            return new EvaluationTarget(
                    EvaluationLog.PracticeSource.RECOMMENDED,
                    sentence.getSentenceId(),
                    sentence.getOriginalText(),
                    sentence.getStandardPronunciation()
            );
        }

        String originalText = request.text().trim();
        return new EvaluationTarget(
                EvaluationLog.PracticeSource.CUSTOM,
                null,
                originalText,
                evaluationService.convertToStandardPronunciation(originalText)
        );
    }

    private void validateTarget(EvaluationJobRequest request) {
        boolean hasSentence = request.sentenceId() != null;
        boolean hasText = request.text() != null && !request.text().isBlank();
        if (hasSentence == hasText) {
            throw new IllegalArgumentException("Exactly one of sentenceId or text is required");
        }
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || !IDEMPOTENCY_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Idempotency-Key must be 8-100 safe characters");
        }
    }

    private String requestHash(EvaluationJobRequest request) {
        String canonical = request.objectKey() + "\n"
                + (request.sentenceId() == null ? "" : request.sentenceId()) + "\n"
                + (request.text() == null ? "" : request.text().trim());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private EvaluationJobResponse toResponse(EvaluationJob job) {
        return new EvaluationJobResponse(
                job.getJobId(),
                job.getStatus(),
                readResult(job.getResultPayload()),
                job.getErrorCode(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }

    private PracticeResultResponse readResult(String payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, PracticeResultResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored evaluation result is invalid", exception);
        }
    }

    public record EvaluationTarget(
            EvaluationLog.PracticeSource source,
            Long sentenceId,
            String originalText,
            String standardPronunciation
    ) {
    }
}
