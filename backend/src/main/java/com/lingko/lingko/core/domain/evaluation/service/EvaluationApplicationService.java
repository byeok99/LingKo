package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import com.lingko.lingko.core.domain.sentence.entity.RecommendedSentence;
import com.lingko.lingko.core.domain.sentence.exception.SentenceNotFoundException;
import com.lingko.lingko.core.domain.sentence.repository.RecommendedSentenceRepository;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 인증 사용자 평가의 문장 확인, 쿼터 예약, 외부 평가, 결과 저장과 보상을 조율한다.
 *
 * 외부 API 호출을 DB 트랜잭션 밖에 두면서도 실패 시 예약을 복구하도록 업무 흐름을 한곳에 모은다.
 */
@Service
public class EvaluationApplicationService {

    private final EvaluationService evaluationService;
    private final EvaluationCompletionService completionService;
    private final PracticeQuotaService quotaService;
    private final UserRepository userRepository;
    private final RecommendedSentenceRepository sentenceRepository;

    public EvaluationApplicationService(
            EvaluationService evaluationService,
            EvaluationCompletionService completionService,
            PracticeQuotaService quotaService,
            UserRepository userRepository,
            RecommendedSentenceRepository sentenceRepository
    ) {
        this.evaluationService = evaluationService;
        this.completionService = completionService;
        this.quotaService = quotaService;
        this.userRepository = userRepository;
        this.sentenceRepository = sentenceRepository;
    }

    public PracticeResultResponse evaluate(
            Long userId,
            MultipartFile audio,
            Long sentenceId,
            String text
    ) {
        User user = findAuthenticatedUser(userId);
        EvaluationTarget target = resolveTarget(sentenceId, text);
        PracticeQuotaService.PracticeQuotaReservation reservation =
                quotaService.reservePractice(userId);

        try {
            PracticeResultResponse result =
                    evaluationService.evaluatePronunciation(audio, target.standardPronunciation());
            completionService.complete(toSaveCommand(user, target, result), reservation);
            return result;
        } catch (RuntimeException exception) {
            releaseReservation(reservation, exception);
            throw exception;
        }
    }

    private User findAuthenticatedUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Authenticated user not found"));
    }

    private EvaluationTarget resolveTarget(Long sentenceId, String text) {
        if (sentenceId != null) {
            RecommendedSentence sentence = sentenceRepository.findBySentenceIdAndActiveTrue(sentenceId)
                    .orElseThrow(() -> new SentenceNotFoundException(sentenceId));
            return new EvaluationTarget(
                    EvaluationLog.PracticeSource.RECOMMENDED,
                    sentence.getSentenceId(),
                    sentence.getOriginalText(),
                    sentence.getStandardPronunciation()
            );
        }

        String originalText = text.trim();
        return new EvaluationTarget(
                EvaluationLog.PracticeSource.CUSTOM,
                null,
                originalText,
                evaluationService.convertToStandardPronunciation(originalText)
        );
    }

    private EvaluationPersistenceService.SaveEvaluationResultCommand toSaveCommand(
            User user,
            EvaluationTarget target,
            PracticeResultResponse result
    ) {
        return EvaluationPersistenceService.SaveEvaluationResultCommand.builder()
                .user(user)
                .source(target.source())
                .sentenceId(target.sentenceId())
                .originalText(target.originalText())
                .standardPronunciation(target.standardPronunciation())
                .result(result)
                .build();
    }

    private void releaseReservation(
            PracticeQuotaService.PracticeQuotaReservation reservation,
            RuntimeException originalException
    ) {
        try {
            quotaService.releasePractice(reservation);
        } catch (RuntimeException compensationException) {
            // 원래 실패 원인을 유지하면서 운영자가 쿼터 복구 실패도 추적할 수 있게 보조 예외로 남긴다.
            originalException.addSuppressed(compensationException);
        }
    }

    private record EvaluationTarget(
            EvaluationLog.PracticeSource source,
            Long sentenceId,
            String originalText,
            String standardPronunciation
    ) {
    }
}
