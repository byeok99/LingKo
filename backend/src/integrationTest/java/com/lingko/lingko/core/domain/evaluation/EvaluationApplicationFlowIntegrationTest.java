package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.evaluation.dto.AssessmentResult;
import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationLogRepository;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationApplicationService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationCompletionService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationPersistenceService;
import com.lingko.lingko.core.domain.evaluation.service.SpeechEvaluator;
import com.lingko.lingko.core.domain.quota.entity.DailyPracticeQuota;
import com.lingko.lingko.core.domain.quota.repository.DailyPracticeQuotaRepository;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 실제 Spring transaction과 JPA를 사용해 평가·쿼터·결과 저장의 성공 및 보상 흐름을 검증한다.
 */
@SpringBootTest
class EvaluationApplicationFlowIntegrationTest {

    @Autowired
    private EvaluationApplicationService applicationService;
    @Autowired
    private EvaluationCompletionService completionService;
    @Autowired
    private PracticeQuotaService quotaService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EvaluationLogRepository evaluationLogRepository;
    @Autowired
    private DailyPracticeQuotaRepository quotaRepository;

    @MockitoBean
    private SpeechEvaluator speechEvaluator;

    @BeforeEach
    void cleanDatabase() {
        evaluationLogRepository.deleteAll();
        quotaRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("평가 성공 시 결과가 저장되고 예약 quota가 사용량으로 확정된다")
    void persistsResultAndConfirmsQuota() {
        User user = saveUser("success-user");
        when(speechEvaluator.evaluate(anyString(), eq("안녕하세요")))
                .thenReturn(assessmentResult());

        PracticeResultResponse response = applicationService.evaluate(
                user.getUserIdx(),
                audio(),
                null,
                "안녕하세요."
        );

        assertThat(response.getOverallScore()).isEqualTo(91);
        assertThat(evaluationLogRepository.findByUser_UserIdxOrderByCreatedAtDesc(
                user.getUserIdx(),
                org.springframework.data.domain.PageRequest.of(0, 10)
        )).hasSize(1);
        DailyPracticeQuota quota = todayQuota(user);
        assertThat(quota.getFreeUsed()).isEqualTo(1);
        assertThat(quota.getFreeReserved()).isZero();
        assertThat(quota.remainingPractices()).isEqualTo(4);
    }

    @Test
    @DisplayName("외부 평가 실패 시 결과를 저장하지 않고 예약 quota를 복구한다")
    void releasesQuotaWhenProviderFails() {
        User user = saveUser("failure-user");
        when(speechEvaluator.evaluate(anyString(), eq("안녕하세요")))
                .thenThrow(new IllegalStateException("provider unavailable"));

        assertThatThrownBy(() -> applicationService.evaluate(
                user.getUserIdx(),
                audio(),
                null,
                "안녕하세요."
        )).isInstanceOf(VideoGenerationException.class);

        assertThat(evaluationLogRepository.count()).isZero();
        DailyPracticeQuota quota = todayQuota(user);
        assertThat(quota.getFreeUsed()).isZero();
        assertThat(quota.getFreeReserved()).isZero();
        assertThat(quota.remainingPractices()).isEqualTo(5);
    }

    @Test
    @DisplayName("쿼터 확정 실패 시 같은 트랜잭션의 평가 결과 저장도 rollback된다")
    void rollsBackResultWhenQuotaConfirmationFails() {
        User user = saveUser("confirmation-failure-user");
        PracticeQuotaService.PracticeQuotaReservation missingReservation =
                new PracticeQuotaService.PracticeQuotaReservation(
                        user.getUserIdx(),
                        LocalDate.now(PracticeQuotaService.SERVICE_ZONE),
                        PracticeQuotaService.QuotaSource.FREE
                );
        EvaluationPersistenceService.SaveEvaluationResultCommand command =
                EvaluationPersistenceService.SaveEvaluationResultCommand.builder()
                        .user(user)
                        .source(EvaluationLog.PracticeSource.CUSTOM)
                        .originalText("안녕하세요.")
                        .standardPronunciation("안녕하세요.")
                        .result(PracticeResultResponse.builder()
                                .overallScore(91)
                                .characters(List.of())
                                .weakCharacters(List.of())
                                .build())
                        .build();

        assertThatThrownBy(() -> completionService.complete(command, missingReservation))
                .isInstanceOf(IllegalStateException.class);

        assertThat(evaluationLogRepository.count()).isZero();
    }

    private User saveUser(String socialId) {
        return userRepository.save(User.builder()
                .socialId(socialId)
                .socialType(User.SocialType.GOOGLE)
                .build());
    }

    private DailyPracticeQuota todayQuota(User user) {
        LocalDate date = quotaService.getTodayQuota(user.getUserIdx()).date();
        return quotaRepository.findByUserUserIdxAndQuotaDate(user.getUserIdx(), date).orElseThrow();
    }

    private MockMultipartFile audio() {
        return new MockMultipartFile(
                "audio",
                "recording.wav",
                "audio/wav",
                new byte[]{1, 2, 3}
        );
    }

    private AssessmentResult assessmentResult() {
        return AssessmentResult.builder()
                .accuracyScore(92.0)
                .fluencyScore(90.0)
                .completenessScore(94.0)
                .pronunciationScore(91.0)
                .recognizedText("안녕하세요.")
                .characterScoresAvailable(false)
                .characterScores(List.of())
                .build();
    }
}
