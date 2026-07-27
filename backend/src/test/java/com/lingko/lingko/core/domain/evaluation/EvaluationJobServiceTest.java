package com.lingko.lingko.core.domain.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.api.evaluation.dto.EvaluationJobRequest;
import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.exception.EvaluationJobConflictException;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationJobRepository;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationAudioStorage;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobCreationService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import com.lingko.lingko.core.domain.sentence.repository.RecommendedSentenceRepository;
import com.lingko.lingko.core.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 평가 작업 생성의 사용자 소유권과 Idempotency 응답 재사용 계약을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class EvaluationJobServiceTest {

    @Mock
    private EvaluationAudioStorage audioStorage;
    @Mock
    private EvaluationJobRepository jobRepository;
    @Mock
    private EvaluationJobCreationService creationService;
    @Mock
    private RecommendedSentenceRepository sentenceRepository;
    @Mock
    private EvaluationService evaluationService;

    private EvaluationJobService jobService;

    @BeforeEach
    void setUp() {
        jobService = new EvaluationJobService(
                audioStorage,
                jobRepository,
                creationService,
                sentenceRepository,
                evaluationService,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    @Test
    @DisplayName("완료된 동일 요청 재호출은 삭제된 S3 object를 확인하지 않고 기존 결과를 반환한다")
    void reusesCompletedIdempotentResponse() {
        EvaluationJobRequest request = new EvaluationJobRequest(
                "evaluation-audio/7/audio.wav",
                null,
                "안녕하세요."
        );
        EvaluationJob existing = existingJob(request);
        when(jobRepository.findByUserUserIdxAndIdempotencyKey(7L, "evaluation-key"))
                .thenReturn(Optional.of(existing));

        assertThat(jobService.createJob(7L, "evaluation-key", request).jobId())
                .isEqualTo("job-id");

        verify(audioStorage, never()).validateUploaded(7L, request.objectKey());
        verify(creationService, never()).create(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("동일 Idempotency Key에 다른 payload를 사용하면 409 대상 충돌로 거부한다")
    void rejectsDifferentPayloadForSameKey() {
        EvaluationJobRequest original = new EvaluationJobRequest(
                "evaluation-audio/7/audio.wav",
                null,
                "안녕하세요."
        );
        EvaluationJob existing = existingJob(original);
        when(jobRepository.findByUserUserIdxAndIdempotencyKey(7L, "evaluation-key"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> jobService.createJob(
                7L,
                "evaluation-key",
                new EvaluationJobRequest(
                        "evaluation-audio/7/other.wav",
                        null,
                        "안녕하세요."
                )
        )).isInstanceOf(EvaluationJobConflictException.class);
    }

    @Test
    @DisplayName("완료 작업에 저장된 평가 결과 JSON을 응답 DTO로 복원한다")
    void restoresCompletedResultPayload() throws Exception {
        EvaluationJobRequest request = new EvaluationJobRequest(
                "evaluation-audio/7/audio.wav",
                null,
                "안녕하세요."
        );
        EvaluationJob existing = existingJob(request);
        PracticeResultResponse result = PracticeResultResponse.builder()
                .overallScore(91)
                .gradeLabel("좋음")
                .summary("발음이 안정적입니다.")
                .recognizedText("안녕하세요.")
                .characterScoreStatus("AVAILABLE")
                .scoreBreakdown(PracticeResultResponse.ScoreBreakdownResponse.builder()
                        .accuracy(92)
                        .fluency(90)
                        .completeness(91)
                        .build())
                .weakCharacters(java.util.List.of())
                .characters(java.util.List.of())
                .build();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        existing.succeed(
                objectMapper.writeValueAsString(result),
                Instant.parse("2026-07-27T01:01:00Z")
        );
        when(jobRepository.findByJobIdAndUserUserIdx("job-id", 7L))
                .thenReturn(Optional.of(existing));

        assertThat(jobService.getJob(7L, "job-id").result())
                .extracting(
                        PracticeResultResponse::getOverallScore,
                        PracticeResultResponse::getRecognizedText
                )
                .containsExactly(91, "안녕하세요.");
    }

    private EvaluationJob existingJob(EvaluationJobRequest request) {
        return EvaluationJob.create(
                "job-id",
                User.builder().socialId("social").socialType(User.SocialType.GOOGLE).build(),
                "evaluation-key",
                requestHash(request),
                request.objectKey(),
                EvaluationLog.PracticeSource.CUSTOM,
                null,
                request.text(),
                "안녕하세여.",
                LocalDate.of(2026, 7, 27),
                PracticeQuotaService.QuotaSource.FREE,
                Instant.parse("2026-07-27T01:00:00Z")
        );
    }

    private String requestHash(EvaluationJobRequest request) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            String canonical = request.objectKey() + "\n\n" + request.text().trim();
            return java.util.HexFormat.of().formatHex(
                    digest.digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8))
            );
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
