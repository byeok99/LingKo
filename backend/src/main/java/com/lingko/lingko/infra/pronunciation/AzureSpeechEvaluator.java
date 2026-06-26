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
                                // Azure only documents reliable syllable grouping for en-US.
                                // Never present full-text/word scores as Korean character scores.
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
