package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.api.evaluation.dto.ScoreStatus;
import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.evaluation.dto.AssessmentResult;
import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import com.lingko.lingko.core.domain.evaluation.service.GuideMediaResolver;
import com.lingko.lingko.core.domain.evaluation.service.SpeechEvaluator;
import com.lingko.lingko.core.domain.sentence.entity.RecommendedSentence;
import com.lingko.lingko.core.domain.sentence.repository.RecommendedSentenceRepository;
import com.lingko.lingko.core.util.SyllableMappingUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Evaluation 서비스 Result Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
class EvaluationServiceResultTest {

    private final SyllableMappingUtil mappingUtil = mock(SyllableMappingUtil.class);
    private final SpeechEvaluator speechEvaluator = mock(SpeechEvaluator.class);
    private final RecommendedSentenceRepository sentenceRepository = mock(RecommendedSentenceRepository.class);
    private final EvaluationService service = new EvaluationService(mappingUtil, speechEvaluator, sentenceRepository);

    @Test
    @DisplayName("text 입력은 표준 발음으로 변환한 뒤 SpeechEvaluator 기준 문장으로 사용한다")
    void evaluateWithTextReference() {
        when(speechEvaluator.evaluate(anyString(), eq("바블 머거써요"))).thenReturn(assessmentResult());
        MockMultipartFile audio = wavAudio();

        PracticeResultResponse response = service.evaluatePronunciation(audio, null, "밥을 먹었어요.");

        assertThat(response.getOverallScore()).isEqualTo(87);
        assertThat(response.getGradeLabel()).isEqualTo("Good");
        assertThat(response.getScoreBreakdown().getAccuracy()).isEqualTo(88);
        assertThat(response.getRecognizedText()).isEqualTo("바블 머거써요.");
        assertThat(response.getSummary()).contains("바블 머거써요.");
    }

    @Test
    @DisplayName("sentenceId 입력은 추천 문장의 표준 발음을 SpeechEvaluator 기준 문장으로 사용한다")
    void evaluateWithSentenceReference() {
        RecommendedSentence sentence = RecommendedSentence.builder()
                .sentenceId(1L)
                .originalText("맛있겠다.")
                .build();
        when(sentenceRepository.findBySentenceIdAndActiveTrue(1L)).thenReturn(Optional.of(sentence));
        when(speechEvaluator.evaluate(anyString(), eq("마싣껟따"))).thenReturn(assessmentResult());
        MockMultipartFile audio = wavAudio();

        PracticeResultResponse response = service.evaluatePronunciation(audio, 1L, null);

        assertThat(response.getOverallScore()).isEqualTo(87);
    }

    @Test
    @DisplayName("wav 파일만 지원한다")
    void supportsWavOnly() {
        assertThat(service.isSupportedAudio(
                wavAudio()
        )).isTrue();
        assertThat(service.isSupportedAudio(
                new MockMultipartFile("audio", "recording.mp3", "audio/mpeg", new byte[]{1})
        )).isFalse();
        assertThat(service.isSupportedAudio(
                new MockMultipartFile("audio", "recording.wav", "audio/wav", new byte[]{1, 2, 3})
        )).isFalse();
    }

    @Test
    @DisplayName("PCM WAV fmt 필드의 sampleRate, byteRate, blockAlign, channels, bitsPerSample은 서로 일관되어야 한다")
    void rejectsInconsistentWavFormatFields() {
        assertThat(service.isSupportedAudio(wavAudio(16000, 1, 16, 16000, 2))).isFalse();
        assertThat(service.isSupportedAudio(wavAudio(16000, 1, 16, 32000, 4))).isFalse();
        assertThat(service.isSupportedAudio(wavAudio(0, 1, 16, 0, 2))).isFalse();
        assertThat(service.isSupportedAudio(wavAudio(16000, 2, 16, 64000, 4))).isFalse();
        assertThat(service.isSupportedAudio(wavAudio(16000, 1, 8, 16000, 1))).isFalse();
    }

    @Test
    @DisplayName("신뢰할 수 있는 Azure 글자 점수로 weakCharacters를 산출한다")
    void mapsReliableCharacterScores() {
        AssessmentResult result = AssessmentResult.builder()
                .accuracyScore(70.0)
                .fluencyScore(80.0)
                .completenessScore(90.0)
                .pronunciationScore(78.0)
                .recognizedText("가나")
                .characterScoresAvailable(true)
                .characterScores(List.of(
                        new AssessmentResult.CharacterScore(0, "가", 92.0),
                        new AssessmentResult.CharacterScore(1, "나", 55.0)
                ))
                .build();
        when(speechEvaluator.evaluate(anyString(), eq("가나"))).thenReturn(result);

        PracticeResultResponse response = service.evaluatePronunciation(wavAudio(), null, "가나");

        assertThat(response.getCharacterScoreStatus()).isEqualTo(ScoreStatus.AVAILABLE);
        assertThat(response.getCharacters()).extracting("score").containsExactly(92, 55);
        assertThat(response.getCharacters()).extracting("scoreStatus").containsOnly(ScoreStatus.AVAILABLE);
        assertThat(response.getWeakCharacters()).extracting("text").containsExactly("나");
        assertThat(response.getWords()).singleElement().satisfies(word ->
                assertThat(word.getSyllables()).extracting("score").containsOnlyNulls()
        );
    }

    @Test
    @DisplayName("Azure가 한국어 글자 매핑을 제공하지 않으면 전체 점수를 글자 점수로 복제하지 않는다")
    void exposesCharacterScoreFallback() {
        when(speechEvaluator.evaluate(anyString(), eq("가나"))).thenReturn(assessmentResult());

        PracticeResultResponse response = service.evaluatePronunciation(wavAudio(), null, "가나");

        assertThat(response.getCharacterScoreStatus()).isEqualTo(ScoreStatus.UNAVAILABLE);
        assertThat(response.getCharacters()).extracting("score").containsOnlyNulls();
        assertThat(response.getCharacters()).extracting("scoreStatus").containsOnly(ScoreStatus.UNAVAILABLE);
        assertThat(response.getWeakCharacters()).isEmpty();
    }

    @Test
    @DisplayName("신뢰할 수 있는 단어 점수를 기준 공백 단위로 묶고 음절은 가이드로만 제공한다")
    void groupsWordScoresWithSyllableGuides() {
        AssessmentResult result = AssessmentResult.builder()
                .accuracyScore(82.0)
                .fluencyScore(84.0)
                .completenessScore(90.0)
                .pronunciationScore(85.0)
                .recognizedText("김치찌개 하나 주세요")
                .characterScoresAvailable(false)
                .characterScores(List.of())
                .wordScoresAvailable(true)
                .wordScores(List.of(
                        new AssessmentResult.WordScore(0, "김치찌개", 82.0),
                        new AssessmentResult.WordScore(1, "하나", 91.0),
                        new AssessmentResult.WordScore(2, "주세요", 77.0)
                ))
                .build();
        when(speechEvaluator.evaluate(anyString(), eq("김치찌개 하나 주세요"))).thenReturn(result);

        PracticeResultResponse response = service.evaluatePronunciation(
                wavAudio(),
                null,
                "김치찌개 하나 주세요"
        );

        assertThat(response.getWordScoreStatus()).isEqualTo(ScoreStatus.AVAILABLE);
        // 화면이 어절·음절마다 로마자를 병기하므로 응답에 함께 실려야 한다.
        assertThat(response.getWords())
                .allSatisfy(word -> assertThat(word.getRomanization()).isNotBlank());
        assertThat(response.getCharacters())
                .allSatisfy(character -> assertThat(character.getRomanization()).isNotBlank());
        assertThat(response.getWords()).extracting("text").containsExactly("김치찌개", "하나", "주세요");
        assertThat(response.getWords()).extracting("score").containsExactly(82, 91, 77);
        assertThat(response.getWords()).extracting(word -> word.getSyllables().size())
                .containsExactly(4, 2, 3);
        assertThat(response.getWords().get(0).getSyllables())
                .extracting("text")
                .containsExactly("김", "치", "찌", "개");
        assertThat(response.getWords().get(0).getSyllables())
                .extracting("score")
                .containsOnlyNulls();
    }

    @Test
    @DisplayName("글자 점수가 없어도 평가 결과의 김 가이드는 초중종성 이동 영상 URL을 사용한다")
    void usesTransitionVideosForKimResultWithoutCharacterScores() {
        GuideMediaResolver guideMediaResolver = mock(GuideMediaResolver.class);
        EvaluationService videoGuideService = new EvaluationService(
                mappingUtil,
                speechEvaluator,
                sentenceRepository,
                guideMediaResolver
        );
        AssessmentResult result = AssessmentResult.builder()
                .accuracyScore(70.0)
                .fluencyScore(75.0)
                .completenessScore(80.0)
                .pronunciationScore(74.0)
                .recognizedText("김")
                .characterScoresAvailable(false)
                .characterScores(List.of())
                .build();
        when(speechEvaluator.evaluate(anyString(), eq("김"))).thenReturn(result);
        when(guideMediaResolver.resolveForEvaluation(
                "김",
                List.of("ㄱ", "ㅣ", "ㅁ"),
                VideoType.MOUTH
        )).thenReturn("https://guides/videos/mouth-kim.mp4");
        when(guideMediaResolver.resolveForEvaluation(
                "김",
                List.of("ㄱ", "ㅣ", "ㅁ"),
                VideoType.TONGUE
        )).thenReturn("https://guides/videos/tongue-kim.mp4");

        PracticeResultResponse response = videoGuideService.evaluatePronunciation(
                wavAudio(),
                null,
                "김"
        );

        assertThat(response.getCharacters()).singleElement().satisfies(character -> {
            assertThat(character.getScoreStatus()).isEqualTo(ScoreStatus.UNAVAILABLE);
            assertThat(character.getPhonemes()).containsExactly("ㄱ", "ㅣ", "ㅁ");
            assertThat(character.getMouthGuideUrl()).endsWith("mouth-kim.mp4");
            assertThat(character.getTongueGuideUrl()).endsWith("tongue-kim.mp4");
        });
    }

    @Test
    @DisplayName("외부 평가가 실패해도 임시 WAV 파일을 삭제한다")
    void deletesTemporaryFileAfterExternalFailure() {
        AtomicReference<Path> evaluatedPath = new AtomicReference<>();
        when(speechEvaluator.evaluate(anyString(), eq("가나"))).thenAnswer(invocation -> {
            evaluatedPath.set(Path.of(invocation.getArgument(0, String.class)));
            assertThat(Files.exists(evaluatedPath.get())).isTrue();
            throw new IllegalStateException("Azure unavailable");
        });

        assertThatThrownBy(() -> service.evaluatePronunciation(wavAudio(), null, "가나"))
                .hasMessage("Speech evaluation failed");
        assertThat(evaluatedPath.get()).isNotNull();
        assertThat(Files.exists(evaluatedPath.get())).isFalse();
    }

    private AssessmentResult assessmentResult() {
        return AssessmentResult.builder()
                .accuracyScore(88.0)
                .fluencyScore(86.0)
                .completenessScore(90.0)
                .pronunciationScore(87.0)
                .recognizedText("바블 머거써요.")
                .characterScoresAvailable(false)
                .characterScores(List.of())
                .build();
    }

    private MockMultipartFile wavAudio() {
        return wavAudio(16000, 1, 16, 32000, 2);
    }

    private MockMultipartFile wavAudio(int sampleRate, int channels, int bitsPerSample, int byteRate, int blockAlign) {
        byte[] bytes = new byte[45];
        writeAscii(bytes, 0, "RIFF");
        writeLittleEndianInt(bytes, 4, bytes.length - 8);
        writeAscii(bytes, 8, "WAVE");
        writeAscii(bytes, 12, "fmt ");
        writeLittleEndianInt(bytes, 16, 16);
        bytes[20] = 1;
        writeLittleEndianShort(bytes, 22, channels);
        writeLittleEndianInt(bytes, 24, sampleRate);
        writeLittleEndianInt(bytes, 28, byteRate);
        writeLittleEndianShort(bytes, 32, blockAlign);
        writeLittleEndianShort(bytes, 34, bitsPerSample);
        writeAscii(bytes, 36, "data");
        writeLittleEndianInt(bytes, 40, 1);
        return new MockMultipartFile("audio", "recording.wav", "audio/wav", bytes);
    }

    private void writeAscii(byte[] target, int offset, String value) {
        for (int index = 0; index < value.length(); index++) {
            target[offset + index] = (byte) value.charAt(index);
        }
    }

    private void writeLittleEndianInt(byte[] target, int offset, int value) {
        target[offset] = (byte) value;
        target[offset + 1] = (byte) (value >>> 8);
        target[offset + 2] = (byte) (value >>> 16);
        target[offset + 3] = (byte) (value >>> 24);
    }

    private void writeLittleEndianShort(byte[] target, int offset, int value) {
        target[offset] = (byte) value;
        target[offset + 1] = (byte) (value >>> 8);
    }
}
