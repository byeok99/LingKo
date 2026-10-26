package com.lingko.lingko.infra.pronunciation;

import com.lingko.lingko.core.config.AzureSettings;
import com.lingko.lingko.core.domain.evaluation.dto.AssessmentResult;
import com.lingko.lingko.core.domain.evaluation.service.SpeechEvaluator;
import com.lingko.lingko.core.domain.evaluation.service.ExternalCallTelemetry;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Azure Speech 평가 응답을 LingKo 발음 평가 결과로 변환한다.
 *
 * 공급자 요청·점수 매핑을 도메인 평가 규칙에서 분리하기 위해 어댑터로 구현했다.
 */
@Slf4j
@Component
public class AzureSpeechEvaluator implements SpeechEvaluator {
    private final AzureSettings settings;
    private final AzurePronunciationResultParser resultParser;
    private final ExternalCallTelemetry telemetry;

    /** 관측 계층이 없는 격리 test에서 동일한 평가 변환 계약을 사용한다. */
    public AzureSpeechEvaluator(AzureSettings settings, AzurePronunciationResultParser resultParser) {
        this(settings, resultParser, Optional.empty());
    }

    /** 일반 API와 최소 prewarm context 모두 동작하도록 관측 bean을 선택 의존성으로 받는다. */
    @Autowired
    public AzureSpeechEvaluator(
            AzureSettings settings,
            AzurePronunciationResultParser resultParser,
            Optional<ExternalCallTelemetry> telemetry
    ) {
        this.settings = settings;
        this.resultParser = resultParser;
        this.telemetry = telemetry.orElse(ExternalCallTelemetry.NOOP);
    }

    @Override
    public AssessmentResult evaluate(String audioPath, String referenceText) {
        long startedAt = System.nanoTime();
        String outcome = "failure";
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
            // 단어 정확도는 고수준 Result API에 없으므로 detailed JSON을 명시적으로 요청한다.
            speechConfig.setOutputFormat(OutputFormat.Detailed);
            try (SpeechRecognizer recognizer = createRecognizer(speechConfig, audioConfig)) {
                pronConfig.applyTo(recognizer);
                try (SpeechRecognitionResult result = recognizer.recognizeOnceAsync().get()) {
                    if (result.getReason() == ResultReason.RecognizedSpeech) {
                        PronunciationAssessmentResult pronResult = PronunciationAssessmentResult.fromResult(result);
                        List<AssessmentResult.WordScore> wordScores = resultParser.parseReliableWordScores(
                                result.getProperties().getProperty(PropertyId.SpeechServiceResponse_JsonResult),
                                referenceText
                        );

                        AssessmentResult assessment = AssessmentResult.builder()
                                .accuracyScore(pronResult.getAccuracyScore())
                                .fluencyScore(pronResult.getFluencyScore())
                                .completenessScore(pronResult.getCompletenessScore())
                                .pronunciationScore(pronResult.getPronunciationScore())
                                .recognizedText(result.getText())
                                // Azure가 신뢰할 수 있는 음절 grouping을 문서화한 locale이 en-US뿐이므로 해당 값을 선택한다.
                                // 전체 문장·단어 점수는 한국어 문자 점수로 신뢰할 수 없어 제공하지 않는다.
                                .characterScoresAvailable(false)
                                .characterScores(List.of())
                                .wordScoresAvailable(!wordScores.isEmpty())
                                .wordScores(wordScores)
                                .build();
                        outcome = "success";
                        return assessment;
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
        } finally {
            telemetry.record("azure", outcome, System.nanoTime() - startedAt);
        }
    }

    protected SpeechRecognizer createRecognizer(SpeechConfig speechConfig, AudioConfig audioConfig) {
        return new SpeechRecognizer(speechConfig, audioConfig);
    }
}
