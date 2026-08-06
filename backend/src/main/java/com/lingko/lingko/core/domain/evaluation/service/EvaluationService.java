package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.api.evaluation.dto.GuideCharacterResponse;
import com.lingko.lingko.api.evaluation.dto.GuideStatus;
import com.lingko.lingko.api.evaluation.dto.ScoreStatus;
import com.lingko.lingko.api.evaluation.dto.PronunciationPrepareResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeWordResultResponse;
import com.lingko.lingko.core.domain.evaluation.dto.AssessmentResult;
import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import com.lingko.lingko.core.domain.sentence.entity.RecommendedSentence;
import com.lingko.lingko.core.domain.sentence.exception.SentenceNotFoundException;
import com.lingko.lingko.core.domain.sentence.repository.RecommendedSentenceRepository;
import com.lingko.lingko.core.util.KoreanPhonemeUtil;
import com.lingko.lingko.core.util.KoreanRomanizationUtil;
import com.lingko.lingko.core.util.PracticeSentenceNormalizer;
import com.lingko.lingko.core.util.SyllableMappingUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Evaluation 업무 규칙을 조율한다.
 *
 * 컨트롤러와 외부 어댑터가 정책을 소유하지 않도록 도메인 서비스에 조율을 집중했다.
 */
@Service
public class EvaluationService {

    public static final long MAX_AUDIO_BYTES = 10L * 1024 * 1024;
    private static final int MIN_WAV_HEADER_BYTES = 44;
    private static final int WEAK_SCORE_THRESHOLD = 80;

    private final GuideMediaResolver guideMediaResolver;
    private final SpeechEvaluator speechEvaluator;
    private final RecommendedSentenceRepository sentenceRepository;

    public enum AudioValidationStatus {
        VALID,
        UNSUPPORTED_TYPE,
        INVALID_WAV
    }

    public EvaluationService(SyllableMappingUtil syllableMappingUtil) {
        this(
                syllableMappingUtil,
                null,
                null,
                new GuideMediaResolver(syllableMappingUtil, null)
        );
    }

    public EvaluationService(
            SyllableMappingUtil syllableMappingUtil,
            SpeechEvaluator speechEvaluator,
            RecommendedSentenceRepository sentenceRepository
    ) {
        this(
                syllableMappingUtil,
                speechEvaluator,
                sentenceRepository,
                new GuideMediaResolver(syllableMappingUtil, null)
        );
    }

    @Autowired
    public EvaluationService(
            SyllableMappingUtil syllableMappingUtil,
            SpeechEvaluator speechEvaluator,
            RecommendedSentenceRepository sentenceRepository,
            GuideMediaResolver guideMediaResolver
    ) {
        this.guideMediaResolver = guideMediaResolver;
        this.speechEvaluator = speechEvaluator;
        this.sentenceRepository = sentenceRepository;
    }

    public String convertToStandardPronunciation(String text) {
        String normalized = normalizeSentenceText(text);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("text must contain supported characters");
        }
        return KoreanPhonemeUtil.toPronunciation(normalized);
    }

    private String normalizeSentenceText(String text) {
        return PracticeSentenceNormalizer.normalize(text);
    }

    public PronunciationPrepareResponse prepareCustomSentence(String text) {
        String normalized = normalizeSentenceText(text);
        String standardPronunciation = convertToStandardPronunciation(normalized);

        return PronunciationPrepareResponse.builder()
                .sentence(PronunciationPrepareResponse.SentenceResponse.builder()
                        .sentenceId(null)
                        .source("CUSTOM")
                        .originalText(normalized)
                        .standardPronunciation(standardPronunciation)
                        .romanizedPronunciation(KoreanRomanizationUtil.romanize(standardPronunciation))
                        .translation("Practice with your own sentence.")
                        .categoryLabel("Free practice")
                        .learningPoint("Linking across syllables")
                        .initialScore(0)
                        .characters(buildGuideCharacters(standardPronunciation))
                        .build())
                .build();
    }

    public List<GuideCharacterResponse> buildGuideCharacters(String standardPronunciation) {
        // Code points preserve complete Unicode characters; char iteration could split supplementary text.
        List<GuideCharacterResponse> characters = new ArrayList<>();
        int position = 0;

        for (String character : normalizeSentenceText(standardPronunciation).codePoints()
                .mapToObj(Character::toString)
                .filter(value -> !value.isBlank())
                .toList()) {
            List<String> phonemes = KoreanPhonemeUtil.toPhonemeList(character);
            String mouthGuideUrl = resolveGuideUrl(
                    phonemes,
                    VideoType.MOUTH
            );
            String tongueGuideUrl = resolveGuideUrl(
                    phonemes,
                    VideoType.TONGUE
            );
            String guideType = resolveGuideType(mouthGuideUrl, tongueGuideUrl);

            characters.add(GuideCharacterResponse.builder()
                    .position(position)
                    .text(character)
                    .pronunciationText(character)
                    .romanization(KoreanRomanizationUtil.romanizeSegment(character))
                    .phonemes(phonemes)
                    .guideType(guideType)
                    .guideStatus("NONE".equals(guideType) ? GuideStatus.MISSING : GuideStatus.AVAILABLE)
                    .mouthGuideUrl(mouthGuideUrl)
                    .tongueGuideUrl(tongueGuideUrl)
                    .note(resolveArticulationNote(guideType))
                    .build());
            position++;
        }

        return characters;
    }

    private String resolveGuideUrl(
            List<String> phonemes,
            VideoType videoType
    ) {
        return guideMediaResolver.resolveStatic(phonemes, videoType);
    }

    private GuideCharacterResponse withTransitionGuides(GuideCharacterResponse character) {
        String mouthGuideUrl = guideMediaResolver.resolveForEvaluation(
                character.getText(),
                character.getPhonemes(),
                VideoType.MOUTH
        );
        String tongueGuideUrl = guideMediaResolver.resolveForEvaluation(
                character.getText(),
                character.getPhonemes(),
                VideoType.TONGUE
        );
        String guideType = resolveGuideType(mouthGuideUrl, tongueGuideUrl);

        return GuideCharacterResponse.builder()
                .position(character.getPosition())
                .text(character.getText())
                .pronunciationText(character.getPronunciationText())
                .romanization(character.getRomanization())
                .phonemes(character.getPhonemes())
                .guideType(guideType)
                .guideStatus("NONE".equals(guideType) ? GuideStatus.MISSING : GuideStatus.AVAILABLE)
                .mouthGuideUrl(mouthGuideUrl)
                .tongueGuideUrl(tongueGuideUrl)
                .note(character.getNote())
                .build();
    }

    /**
     * 가이드가 있을 때만 어떤 부위에 집중할지 알려주고, 없으면 빈 문자열을 반환한다.
     *
     * 이전에는 guideType을 그대로 문장에 끼워 넣어 가이드가 없는 글자에
     * "Focus on none placement"라는 문장이 사용자에게 노출됐다. 표시할 가이드가 없으면
     * 안내할 대상도 없으므로 문구를 만들지 않고, 클라이언트가 빈 값을 감추게 한다.
     */
    private String resolveArticulationNote(String guideType) {
        return switch (guideType) {
            case "TONGUE" -> "Focus on where your tongue touches.";
            case "MOUTH" -> "Focus on your lip and jaw opening.";
            default -> "";
        };
    }

    private String resolveGuideType(String mouthGuideUrl, String tongueGuideUrl) {
        if (tongueGuideUrl != null) {
            return "TONGUE";
        }

        if (mouthGuideUrl != null) {
            return "MOUTH";
        }

        return "NONE";
    }

    public boolean isSupportedAudio(MultipartFile audio) {
        return validateAudio(audio) == AudioValidationStatus.VALID;
    }

    public AudioValidationStatus validateAudio(MultipartFile audio) {
        String filename = audio.getOriginalFilename();
        String contentType = audio.getContentType();
        boolean wavName = filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".wav");
        boolean wavType = contentType == null
                || contentType.equalsIgnoreCase("audio/wav")
                || contentType.equalsIgnoreCase("audio/x-wav")
                || contentType.equalsIgnoreCase("audio/vnd.wave")
                || contentType.equalsIgnoreCase("application/octet-stream");

        // 확장자와 MIME은 1차 filter일 뿐이며 아래에서 RIFF/PCM 구조를 다시 검증한다.
        if (!wavName || !wavType || audio.getSize() < MIN_WAV_HEADER_BYTES) {
            return !wavName || !wavType
                    ? AudioValidationStatus.UNSUPPORTED_TYPE
                    : AudioValidationStatus.INVALID_WAV;
        }

        try (InputStream input = audio.getInputStream()) {
            return hasValidPcmWavHeader(input, audio.getSize())
                    ? AudioValidationStatus.VALID
                    : AudioValidationStatus.INVALID_WAV;
        } catch (IOException exception) {
            return AudioValidationStatus.INVALID_WAV;
        }
    }

    private boolean hasValidPcmWavHeader(InputStream input, long fileSize) throws IOException {
        // 유효한 WAV에도 metadata chunk가 있을 수 있어 고정 44-byte header를 가정하지 않고 chunk를 parsing한다.
        byte[] riffHeader = input.readNBytes(12);
        if (riffHeader.length != 12
                || !matchesAscii(riffHeader, 0, "RIFF")
                || !matchesAscii(riffHeader, 8, "WAVE")) {
            return false;
        }

        long declaredFileSize = littleEndianUnsignedInt(riffHeader, 4) + 8;
        if (declaredFileSize > fileSize || declaredFileSize < MIN_WAV_HEADER_BYTES) {
            return false;
        }

        boolean validFormat = false;
        long consumed = 12;
        while (consumed + 8 <= declaredFileSize) {
            byte[] chunkHeader = input.readNBytes(8);
            if (chunkHeader.length != 8) {
                return false;
            }
            consumed += 8;
            long chunkSize = littleEndianUnsignedInt(chunkHeader, 4);
            if (chunkSize > declaredFileSize - consumed) {
                return false;
            }

            if (matchesAscii(chunkHeader, 0, "fmt ")) {
                if (chunkSize < 16) {
                    return false;
                }
                byte[] format = input.readNBytes(16);
                if (format.length != 16) {
                    return false;
                }
                consumed += 16;
                validFormat = hasConsistentPcmFormat(format);
                if (!skipFully(input, chunkSize - 16)) {
                    return false;
                }
                consumed += chunkSize - 16;
            } else if (matchesAscii(chunkHeader, 0, "data")) {
                return validFormat && chunkSize > 0;
            } else {
                if (!skipFully(input, chunkSize)) {
                    return false;
                }
                consumed += chunkSize;
            }

            if ((chunkSize & 1) == 1 && consumed < declaredFileSize) {
                if (!skipFully(input, 1)) {
                    return false;
                }
                consumed++;
            }
        }
        return false;
    }

    private boolean skipFully(InputStream input, long byteCount) throws IOException {
        long remaining = byteCount;
        while (remaining > 0) {
            long skipped = input.skip(remaining);
            if (skipped <= 0) {
                if (input.read() == -1) {
                    return false;
                }
                skipped = 1;
            }
            remaining -= skipped;
        }
        return true;
    }

    private boolean hasConsistentPcmFormat(byte[] format) {
        int audioFormat = littleEndianUnsignedShort(format, 0);
        int channels = littleEndianUnsignedShort(format, 2);
        long sampleRate = littleEndianUnsignedInt(format, 4);
        long byteRate = littleEndianUnsignedInt(format, 8);
        int blockAlign = littleEndianUnsignedShort(format, 12);
        int bitsPerSample = littleEndianUnsignedShort(format, 14);
        int expectedBlockAlign = channels * bitsPerSample / 8;
        long expectedByteRate = sampleRate * expectedBlockAlign;

        return audioFormat == 1
                && channels == 1
                && bitsPerSample == 16
                && sampleRate > 0
                && sampleRate <= 48_000
                && blockAlign == expectedBlockAlign
                && byteRate == expectedByteRate;
    }

    private boolean matchesAscii(byte[] bytes, int offset, String expected) {
        byte[] expectedBytes = expected.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        return offset + expectedBytes.length <= bytes.length
                && Arrays.equals(bytes, offset, offset + expectedBytes.length, expectedBytes, 0, expectedBytes.length);
    }

    private long littleEndianUnsignedInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xffL)
                | ((bytes[offset + 1] & 0xffL) << 8)
                | ((bytes[offset + 2] & 0xffL) << 16)
                | ((bytes[offset + 3] & 0xffL) << 24);
    }

    private int littleEndianUnsignedShort(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8);
    }

    public PracticeResultResponse evaluatePronunciation(MultipartFile audio, Long sentenceId, String text) {
        String referenceText = resolveReferenceText(sentenceId, text);
        return evaluatePronunciation(audio, referenceText);
    }

    public PracticeResultResponse evaluatePronunciation(MultipartFile audio, String referenceText) {
        Path tempFile = null;

        try {
            // 공급자 API가 파일 경로를 요구하므로 upload byte를 이 호출 동안만 임시 파일로 저장한다.
            tempFile = Files.createTempFile("lingko-evaluation-", ".wav");
            audio.transferTo(tempFile);

            AssessmentResult assessmentResult = requireSpeechEvaluator()
                    .evaluate(tempFile.toString(), referenceText);

            return toPracticeResult(referenceText, assessmentResult);
        } catch (IOException exception) {
            throw new VideoGenerationException("Failed to store uploaded audio");
        } catch (RuntimeException exception) {
            if (exception instanceof SentenceNotFoundException) {
                throw exception;
            }

            throw new VideoGenerationException("Speech evaluation failed", exception);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // 임시 파일 정리 실패가 정상 평가 결과나 원래 예외를 덮어쓰지 않게 한다.
                }
            }
        }
    }

    /**
     * Worker가 S3에서 받은 로컬 WAV를 다시 복사하지 않고 공급자 평가에 전달한다.
     */
    public PracticeResultResponse evaluatePronunciation(Path audioPath, String referenceText) {
        try {
            long fileSize = Files.size(audioPath);
            try (InputStream input = Files.newInputStream(audioPath)) {
                if (!hasValidPcmWavHeader(input, fileSize)) {
                    throw new VideoGenerationException("Invalid uploaded WAV audio");
                }
            }
            AssessmentResult assessmentResult = requireSpeechEvaluator()
                    .evaluate(audioPath.toString(), referenceText);
            return toPracticeResult(referenceText, assessmentResult);
        } catch (IOException exception) {
            throw new VideoGenerationException("Failed to read uploaded audio", exception);
        } catch (RuntimeException exception) {
            if (exception instanceof VideoGenerationException) {
                throw exception;
            }
            throw new VideoGenerationException("Speech evaluation failed", exception);
        }
    }

    private String resolveReferenceText(Long sentenceId, String text) {
        if (sentenceId != null) {
            RecommendedSentence sentence = requireSentenceRepository()
                    .findBySentenceIdAndActiveTrue(sentenceId)
                    .orElseThrow(() -> new SentenceNotFoundException(sentenceId));
            return convertToStandardPronunciation(sentence.getOriginalText());
        }

        return convertToStandardPronunciation(text.trim());
    }

    private PracticeResultResponse toPracticeResult(String referenceText, AssessmentResult result) {
        int overallScore = toScore(result.getPronunciationScore());
        List<GuideCharacterResponse> guideCharacters = buildGuideCharacters(referenceText);
        // 공급자 문자와 정규화된 기준 문자가 다르면 위치만으로 점수를 연결하지 않는다.
        boolean characterScoresAvailable = hasReliableCharacterScores(guideCharacters, result);
        List<GuideCharacterResponse> characters = guideCharacters.stream()
                .map(character -> {
                    Integer characterScore = characterScoresAvailable
                            ? toScore(result.getCharacterScores().get(character.getPosition()).accuracyScore())
                            : null;
                    // Result에서는 점수 유무와 관계없이 모든 음절을 열 수 있으므로 프레임 전환 자체를 영상 기준으로 삼는다.
                    GuideCharacterResponse resolvedCharacter = withTransitionGuides(character);
                    return GuideCharacterResponse.builder()
                        .position(resolvedCharacter.getPosition())
                        .text(resolvedCharacter.getText())
                        .pronunciationText(resolvedCharacter.getPronunciationText())
                        .romanization(resolvedCharacter.getRomanization())
                        .score(characterScore)
                        .scoreStatus(characterScoresAvailable ? ScoreStatus.AVAILABLE : ScoreStatus.UNAVAILABLE)
                        .phonemes(resolvedCharacter.getPhonemes())
                        .guideType(resolvedCharacter.getGuideType())
                        .guideStatus(resolvedCharacter.getGuideStatus())
                        .mouthGuideUrl(resolvedCharacter.getMouthGuideUrl())
                        .tongueGuideUrl(resolvedCharacter.getTongueGuideUrl())
                        .note(resolvedCharacter.getNote())
                        .build();
                })
                .toList();

        List<GuideCharacterResponse> weakCharacters = characterScoresAvailable
                ? characters.stream()
                        .filter(character -> character.getScore() < WEAK_SCORE_THRESHOLD)
                        .toList()
                : List.of();
        List<PracticeWordResultResponse> words = buildWordResults(referenceText, characters, result);
        boolean wordScoresAvailable = words.stream()
                .allMatch(word -> word.getScoreStatus().isAvailable());

        return PracticeResultResponse.builder()
                .overallScore(overallScore)
                .gradeLabel(resolveGradeLabel(overallScore))
                .summary(resolveSummary(overallScore, result.getRecognizedText()))
                .recognizedText(result.getRecognizedText())
                .characterScoreStatus(characterScoresAvailable ? ScoreStatus.AVAILABLE : ScoreStatus.UNAVAILABLE)
                .wordScoreStatus(wordScoresAvailable ? ScoreStatus.AVAILABLE : ScoreStatus.UNAVAILABLE)
                .scoreBreakdown(PracticeResultResponse.ScoreBreakdownResponse.builder()
                        .accuracy(toScore(result.getAccuracyScore()))
                        .fluency(toScore(result.getFluencyScore()))
                        .completeness(toScore(result.getCompletenessScore()))
                        .build())
                .characters(characters)
                .weakCharacters(weakCharacters)
                .words(words)
                .build();
    }

    /**
     * 기준 문장의 공백을 단어 경계로 사용하고, 음절은 점수 복제 없이 가이드로만 묶는다.
     */
    private List<PracticeWordResultResponse> buildWordResults(
            String referenceText,
            List<GuideCharacterResponse> characters,
            AssessmentResult result
    ) {
        List<String> referenceWords = List.of(referenceText.trim().split("\\s+"));
        boolean scoresAvailable = hasReliableWordScores(referenceWords, result);
        List<PracticeWordResultResponse> words = new ArrayList<>(referenceWords.size());
        int characterOffset = 0;

        for (int position = 0; position < referenceWords.size(); position++) {
            String word = referenceWords.get(position);
            int syllableCount = (int) word.codePoints()
                    .mapToObj(Character::toString)
                    .filter(value -> !value.isBlank())
                    .count();
            if (characterOffset + syllableCount > characters.size()) {
                return fallbackWord(referenceText, characters);
            }

            List<GuideCharacterResponse> syllables = characters
                    .subList(characterOffset, characterOffset + syllableCount)
                    .stream()
                    .map(this::toGuideOnlySyllable)
                    .toList();
            Integer score = scoresAvailable
                    ? toScore(result.getWordScores().get(position).accuracyScore())
                    : null;
            words.add(PracticeWordResultResponse.builder()
                    .position(position)
                    .text(word)
                    .romanization(KoreanRomanizationUtil.romanizeSegment(word))
                    .score(score)
                    .scoreStatus(scoresAvailable ? ScoreStatus.AVAILABLE : ScoreStatus.UNAVAILABLE)
                    .syllables(syllables)
                    .build());
            characterOffset += syllableCount;
        }

        return characterOffset == characters.size()
                ? List.copyOf(words)
                : fallbackWord(referenceText, characters);
    }

    private List<PracticeWordResultResponse> fallbackWord(
            String referenceText,
            List<GuideCharacterResponse> characters
    ) {
        return List.of(PracticeWordResultResponse.builder()
                .position(0)
                .text(referenceText.trim())
                .romanization(KoreanRomanizationUtil.romanizeSegment(referenceText))
                .score(null)
                .scoreStatus(ScoreStatus.UNAVAILABLE)
                .syllables(characters.stream().map(this::toGuideOnlySyllable).toList())
                .build());
    }

    /** 단어 하위 음절은 점수 단위가 아니므로 가이드 metadata만 복사한다. */
    private GuideCharacterResponse toGuideOnlySyllable(GuideCharacterResponse character) {
        return GuideCharacterResponse.builder()
                .position(character.getPosition())
                .text(character.getText())
                .pronunciationText(character.getPronunciationText())
                .romanization(character.getRomanization())
                .score(null)
                .scoreStatus(ScoreStatus.UNAVAILABLE)
                .phonemes(character.getPhonemes())
                .guideType(character.getGuideType())
                .guideStatus(character.getGuideStatus())
                .mouthGuideUrl(character.getMouthGuideUrl())
                .tongueGuideUrl(character.getTongueGuideUrl())
                .note(character.getNote())
                .build();
    }

    private boolean hasReliableWordScores(List<String> referenceWords, AssessmentResult result) {
        List<AssessmentResult.WordScore> scores = result.getWordScores();
        if (!result.isWordScoresAvailable() || scores == null || scores.size() != referenceWords.size()) {
            return false;
        }

        for (int position = 0; position < referenceWords.size(); position++) {
            AssessmentResult.WordScore score = scores.get(position);
            if (score.position() != position
                    || !referenceWords.get(position).equals(score.text())
                    || score.accuracyScore() == null) {
                return false;
            }
        }
        return true;
    }

    private boolean hasReliableCharacterScores(
            List<GuideCharacterResponse> characters,
            AssessmentResult result
    ) {
        List<AssessmentResult.CharacterScore> scores = result.getCharacterScores();
        if (!result.isCharacterScoresAvailable() || scores == null || scores.size() != characters.size()) {
            return false;
        }

        for (int index = 0; index < characters.size(); index++) {
            AssessmentResult.CharacterScore score = scores.get(index);
            GuideCharacterResponse character = characters.get(index);
            if (score.position() != index
                    || !character.getText().equals(score.text())
                    || score.accuracyScore() == null) {
                return false;
            }
        }
        return true;
    }

    private int toScore(Double score) {
        if (score == null) {
            return 0;
        }

        return (int) Math.round(score);
    }

    private String resolveGradeLabel(int score) {
        if (score >= 90) {
            return "Excellent";
        }

        if (score >= 75) {
            return "Good";
        }

        return "Needs work";
    }

    private String resolveSummary(int score, String recognizedText) {
        String scoreSummary;
        if (score >= 90) {
            scoreSummary = "Clear pronunciation. Keep the same rhythm and articulation.";
        } else if (score >= 75) {
            scoreSummary = "Good pronunciation. Review the highlighted sounds and try once more.";
        } else {
            scoreSummary = "Pronunciation needs more practice. Focus on the guide items before retrying.";
        }

        if (recognizedText == null || recognizedText.isBlank()) {
            return scoreSummary;
        }
        return scoreSummary + " Recognized speech: " + recognizedText.trim();
    }

    private SpeechEvaluator requireSpeechEvaluator() {
        if (speechEvaluator == null) {
            throw new VideoGenerationException("Speech evaluator is not configured");
        }

        return speechEvaluator;
    }

    private RecommendedSentenceRepository requireSentenceRepository() {
        if (sentenceRepository == null) {
            throw new SentenceNotFoundException(null);
        }

        return sentenceRepository;
    }
}
