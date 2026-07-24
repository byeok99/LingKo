package com.lingko.lingko.infra.pronunciation;

import com.lingko.lingko.core.config.AzureSettings;
import com.lingko.lingko.core.domain.evaluation.dto.AssessmentResult;
import com.lingko.lingko.core.domain.evaluation.service.SpeechEvaluator;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Azure Speech 평가 응답을 LingKo 발음 평가 결과로 변환한다.
 *
 * 공급자 요청·점수 매핑을 도메인 평가 규칙에서 분리하기 위해 어댑터로 구현했다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AzureSpeechEvaluator implements SpeechEvaluator {
    private final AzureSettings settings;

    @Override
    public AssessmentResult evaluate(String audioPath, String referenceText) {
        try (SpeechConfig speechConfig = SpeechConfig.fromSubscription(
                settings.getSecretKey(), settings.getRegion());
             AudioConfig audioConfig = AudioConfig.fromWavFileInput(audioPath);
             PronunciationAssessmentConfig pronConfig = new PronunciationAssessmentConfig(
                     referenceText,
                     PronunciationAssessmentGradingSystem.HundredMark,
                     PronunciationAssessmentGranularity.Phoneme,
                     false
             )) {
            speechConfig.setSpeechRecognitionLanguage(settings.getLanguage());
            try (SpeechRecognizer recognizer = createRecognizer(speechConfig, audioConfig)) {
                pronConfig.applyTo(recognizer);
                try (SpeechRecognitionResult result = recognizer.recognizeOnceAsync().get()) {
                    if (result.getReason() == ResultReason.RecognizedSpeech) {
                        PronunciationAssessmentResult pronResult = PronunciationAssessmentResult.fromResult(result);

                        return AssessmentResult.builder()
                                .accuracyScore(pronResult.getAccuracyScore())
                                .fluencyScore(pronResult.getFluencyScore())
                                .completenessScore(pronResult.getCompletenessScore())
                                .pronunciationScore(pronResult.getPronunciationScore())
                                .recognizedText(result.getText())
                                // Azure가 신뢰할 수 있는 음절 grouping을 문서화한 locale이 en-US뿐이므로 해당 값을 선택한다.
                                // 전체 문장·단어 점수는 한국어 문자 점수로 신뢰할 수 없어 제공하지 않는다.
                                .characterScoresAvailable(false)
                                .characterScores(List.of())
                                .build();
                    }
                    throw new IllegalStateException("음성 인식에 실패했습니다: " + result.getReason());
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("발음 평가가 중단되었습니다", exception);
        } catch (Exception e) {
            log.error("Azure 평가 중 에러 발생", e);
            throw new IllegalStateException("발음 평가 실패", e);
        }
    }

    protected SpeechRecognizer createRecognizer(SpeechConfig speechConfig, AudioConfig audioConfig) {
        return new SpeechRecognizer(speechConfig, audioConfig);
    }
}
