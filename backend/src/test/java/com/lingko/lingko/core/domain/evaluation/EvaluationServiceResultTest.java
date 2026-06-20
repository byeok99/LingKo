package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.evaluation.dto.AssessmentResult;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import com.lingko.lingko.core.domain.evaluation.service.SpeechEvaluator;
import com.lingko.lingko.core.domain.sentence.entity.RecommendedSentence;
import com.lingko.lingko.core.domain.sentence.repository.RecommendedSentenceRepository;
import com.lingko.lingko.core.util.SyllableMappingUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EvaluationServiceResultTest {

    private final SyllableMappingUtil mappingUtil = mock(SyllableMappingUtil.class);
    private final SpeechEvaluator speechEvaluator = mock(SpeechEvaluator.class);
    private final RecommendedSentenceRepository sentenceRepository = mock(RecommendedSentenceRepository.class);
    private final EvaluationService service = new EvaluationService(mappingUtil, speechEvaluator, sentenceRepository);

    @Test
    @DisplayName("text 입력은 표준 발음으로 변환한 뒤 SpeechEvaluator 기준 문장으로 사용한다")
    void evaluateWithTextReference() {
        when(speechEvaluator.evaluate(anyString(), eq("바블 머거써요."))).thenReturn(assessmentResult());
        MockMultipartFile audio = new MockMultipartFile("audio", "recording.wav", "audio/wav", new byte[]{1});

        PracticeResultResponse response = service.evaluatePronunciation(audio, null, "밥을 먹었어요.");

        assertThat(response.getOverallScore()).isEqualTo(87);
        assertThat(response.getGradeLabel()).isEqualTo("Good");
        assertThat(response.getScoreBreakdown().getAccuracy()).isEqualTo(88);
    }

    @Test
    @DisplayName("sentenceId 입력은 추천 문장의 표준 발음을 SpeechEvaluator 기준 문장으로 사용한다")
    void evaluateWithSentenceReference() {
        RecommendedSentence sentence = RecommendedSentence.builder()
                .sentenceId(1L)
                .standardPronunciation("마싯게따.")
                .build();
        when(sentenceRepository.findBySentenceIdAndActiveTrue(1L)).thenReturn(Optional.of(sentence));
        when(speechEvaluator.evaluate(anyString(), eq("마싯게따."))).thenReturn(assessmentResult());
        MockMultipartFile audio = new MockMultipartFile("audio", "recording.wav", "audio/wav", new byte[]{1});

        PracticeResultResponse response = service.evaluatePronunciation(audio, 1L, null);

        assertThat(response.getOverallScore()).isEqualTo(87);
    }

    @Test
    @DisplayName("wav 파일만 지원한다")
    void supportsWavOnly() {
        assertThat(service.isSupportedAudio(
                new MockMultipartFile("audio", "recording.wav", "audio/wav", new byte[]{1})
        )).isTrue();
        assertThat(service.isSupportedAudio(
                new MockMultipartFile("audio", "recording.mp3", "audio/mpeg", new byte[]{1})
        )).isFalse();
    }

    private AssessmentResult assessmentResult() {
        return AssessmentResult.builder()
                .accuracyScore(88.0)
                .fluencyScore(86.0)
                .completenessScore(90.0)
                .pronunciationScore(87.0)
                .recognizedText("바블 머거써요.")
                .build();
    }
}
