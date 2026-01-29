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

@Slf4j
@Component
@RequiredArgsConstructor
public class AzureSpeechEvaluator implements SpeechEvaluator {
    private final AzureSettings settings;

    @Override
    public AssessmentResult evaluate(String audioPath, String referenceText) {
        // 1. 설정 초기화
        SpeechConfig speechConfig = SpeechConfig.fromSubscription(
                settings.getSecretKey(),
                settings.getRegion()
        );
        AudioConfig audioConfig = AudioConfig.fromWavFileInput(audioPath);

        // 2. 발음 평가 설정
        PronunciationAssessmentConfig pronConfig = new PronunciationAssessmentConfig(
                referenceText,
                PronunciationAssessmentGradingSystem.HundredMark,
                PronunciationAssessmentGranularity.Phoneme,
                false
        );

        // 3. 실행 (Try-with-resources로 리소스 자동 해제)
        try (SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig)) {
            pronConfig.applyTo(recognizer);
            SpeechRecognitionResult result = recognizer.recognizeOnceAsync().get();

            if (result.getReason() == ResultReason.RecognizedSpeech) {
                PronunciationAssessmentResult pronResult = PronunciationAssessmentResult.fromResult(result);

                return AssessmentResult.builder()
                        .accuracyScore(pronResult.getAccuracyScore())
                        .fluencyScore(pronResult.getFluencyScore())
                        .completenessScore(pronResult.getCompletenessScore())
                        .pronunciationScore(pronResult.getPronunciationScore())
                        .recognizedText(result.getText())
                        .build();
            }
            throw new RuntimeException("음성 인식에 실패했습니다: " + result.getReason());
        } catch (Exception e) {
            log.error("Azure 평가 중 에러 발생", e);
            throw new RuntimeException("발음 평가 실패", e);
        }
    }
}