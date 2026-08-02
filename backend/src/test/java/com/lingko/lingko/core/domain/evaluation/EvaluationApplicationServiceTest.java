package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationApplicationService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationCompletionService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationPersistenceService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import com.lingko.lingko.core.domain.sentence.entity.RecommendedSentence;
import com.lingko.lingko.core.domain.sentence.repository.RecommendedSentenceRepository;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 인증 사용자 평가가 쿼터 예약, 외부 평가, 결과 저장을 하나의 업무 흐름으로 조율하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class EvaluationApplicationServiceTest {

    @Mock
    private EvaluationService evaluationService;
    @Mock
    private EvaluationCompletionService completionService;
    @Mock
    private PracticeQuotaService quotaService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RecommendedSentenceRepository sentenceRepository;

    private EvaluationApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new EvaluationApplicationService(
                evaluationService,
                completionService,
                quotaService,
                userRepository,
                sentenceRepository
        );
    }

    @Test
    @DisplayName("추천 문장 평가는 해당 문장의 원문과 표준 발음을 저장하고 예약 쿼터를 확정한다")
    void evaluatesAndPersistsRecommendedSentence() {
        User user = user(7L);
        RecommendedSentence sentence = RecommendedSentence.builder()
                .sentenceId(12L)
                .originalText("맛있겠다.")
                .active(true)
                .build();
        PracticeResultResponse result = result(91);
        PracticeQuotaService.PracticeQuotaReservation reservation = reservation(
                PracticeQuotaService.QuotaSource.FREE
        );
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(sentenceRepository.findBySentenceIdAndActiveTrue(12L)).thenReturn(Optional.of(sentence));
        when(evaluationService.convertToStandardPronunciation("맛있겠다")).thenReturn("마싣껟따");
        when(quotaService.reservePractice(7L)).thenReturn(reservation);
        when(evaluationService.evaluatePronunciation(any(MultipartFile.class), eq("마싣껟따")))
                .thenReturn(result);

        PracticeResultResponse response = applicationService.evaluate(7L, audio(), 12L, null);

        assertThat(response).isSameAs(result);
        ArgumentCaptor<EvaluationPersistenceService.SaveEvaluationResultCommand> commandCaptor =
                ArgumentCaptor.forClass(EvaluationPersistenceService.SaveEvaluationResultCommand.class);
        verify(completionService).complete(commandCaptor.capture(), org.mockito.ArgumentMatchers.eq(reservation));
        EvaluationPersistenceService.SaveEvaluationResultCommand command = commandCaptor.getValue();
        assertThat(command.user()).isSameAs(user);
        assertThat(command.source()).isEqualTo(EvaluationLog.PracticeSource.RECOMMENDED);
        assertThat(command.sentenceId()).isEqualTo(12L);
        assertThat(command.originalText()).isEqualTo("맛있겠다");
        assertThat(command.standardPronunciation()).isEqualTo("마싣껟따");
        assertThat(command.result()).isSameAs(result);
        verify(quotaService, never()).releasePractice(any());
    }

    @Test
    @DisplayName("사용자 문장 평가는 정규화한 원문과 계산한 표준 발음을 저장한다")
    void evaluatesAndPersistsCustomSentence() {
        User user = user(7L);
        PracticeResultResponse result = result(82);
        PracticeQuotaService.PracticeQuotaReservation reservation = reservation(
                PracticeQuotaService.QuotaSource.FREE
        );
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(evaluationService.convertToStandardPronunciation("안녕하세요")).thenReturn("안녕하세여");
        when(quotaService.reservePractice(7L)).thenReturn(reservation);
        when(evaluationService.evaluatePronunciation(any(MultipartFile.class), eq("안녕하세여")))
                .thenReturn(result);

        applicationService.evaluate(7L, audio(), null, "  안녕하세요.  ");

        ArgumentCaptor<EvaluationPersistenceService.SaveEvaluationResultCommand> commandCaptor =
                ArgumentCaptor.forClass(EvaluationPersistenceService.SaveEvaluationResultCommand.class);
        verify(completionService).complete(commandCaptor.capture(), org.mockito.ArgumentMatchers.eq(reservation));
        EvaluationPersistenceService.SaveEvaluationResultCommand command = commandCaptor.getValue();
        assertThat(command.source()).isEqualTo(EvaluationLog.PracticeSource.CUSTOM);
        assertThat(command.sentenceId()).isNull();
        assertThat(command.originalText()).isEqualTo("안녕하세요");
        assertThat(command.standardPronunciation()).isEqualTo("안녕하세여");
    }

    @Test
    @DisplayName("외부 발음 평가 실패 시 예약한 쿼터를 복구하고 결과를 저장하지 않는다")
    void releasesQuotaWhenEvaluationFails() {
        User user = user(7L);
        PracticeQuotaService.PracticeQuotaReservation reservation = reservation(
                PracticeQuotaService.QuotaSource.FREE
        );
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(evaluationService.convertToStandardPronunciation("안녕하세요")).thenReturn("안녕하세여");
        when(quotaService.reservePractice(7L)).thenReturn(reservation);
        when(evaluationService.evaluatePronunciation(any(MultipartFile.class), eq("안녕하세여")))
                .thenThrow(new IllegalStateException("provider failed"));

        assertThatThrownBy(() -> applicationService.evaluate(7L, audio(), null, "안녕하세요."))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("provider failed");

        verify(quotaService).releasePractice(reservation);
        verify(completionService, never()).complete(any(), any());
    }

    @Test
    @DisplayName("결과 저장 실패 시 예약한 쿼터를 복구한다")
    void releasesQuotaWhenPersistenceFails() {
        User user = user(7L);
        PracticeQuotaService.PracticeQuotaReservation reservation = reservation(
                PracticeQuotaService.QuotaSource.REWARDED
        );
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(evaluationService.convertToStandardPronunciation("안녕하세요")).thenReturn("안녕하세여");
        when(quotaService.reservePractice(7L)).thenReturn(reservation);
        when(evaluationService.evaluatePronunciation(any(MultipartFile.class), eq("안녕하세여")))
                .thenReturn(result(82));
        org.mockito.Mockito.doThrow(new IllegalStateException("database failed"))
                .when(completionService)
                .complete(any(), any());

        assertThatThrownBy(() -> applicationService.evaluate(7L, audio(), null, "안녕하세요."))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database failed");

        verify(quotaService).releasePractice(reservation);
    }

    private User user(Long userId) {
        return User.builder()
                .userIdx(userId)
                .socialId("social-id")
                .socialType(User.SocialType.GOOGLE)
                .build();
    }

    private PracticeQuotaService.PracticeQuotaReservation reservation(
            PracticeQuotaService.QuotaSource source
    ) {
        return new PracticeQuotaService.PracticeQuotaReservation(
                7L,
                LocalDate.of(2026, 7, 24),
                source
        );
    }

    private MockMultipartFile audio() {
        return new MockMultipartFile("audio", "recording.wav", "audio/wav", new byte[]{1});
    }

    private PracticeResultResponse result(int score) {
        return PracticeResultResponse.builder()
                .overallScore(score)
                .characters(List.of())
                .weakCharacters(List.of())
                .build();
    }
}
