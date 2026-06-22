package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.api.evaluation.dto.GuideCharacterResponse;
import com.lingko.lingko.api.evaluation.dto.PronunciationPrepareResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.evaluation.dto.AssessmentResult;
import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import com.lingko.lingko.core.domain.sentence.entity.RecommendedSentence;
import com.lingko.lingko.core.domain.sentence.exception.SentenceNotFoundException;
import com.lingko.lingko.core.domain.sentence.repository.RecommendedSentenceRepository;
import com.lingko.lingko.core.util.KoreanPhonemeUtil;
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

@Service
public class EvaluationService {

    public static final long MAX_AUDIO_BYTES = 10L * 1024 * 1024;
    private static final int MIN_WAV_HEADER_BYTES = 44;

    private final SyllableMappingUtil syllableMappingUtil;
    private final SpeechEvaluator speechEvaluator;
    private final RecommendedSentenceRepository sentenceRepository;

    public enum AudioValidationStatus {
        VALID,
        UNSUPPORTED_TYPE,
        INVALID_WAV
    }

    public EvaluationService(SyllableMappingUtil syllableMappingUtil) {
        this(syllableMappingUtil, null, null);
    }

    @Autowired
    public EvaluationService(
            SyllableMappingUtil syllableMappingUtil,
            SpeechEvaluator speechEvaluator,
            RecommendedSentenceRepository sentenceRepository
    ) {
        this.syllableMappingUtil = syllableMappingUtil;
        this.speechEvaluator = speechEvaluator;
        this.sentenceRepository = sentenceRepository;
    }

    public String convertToStandardPronunciation(String text) {
        return KoreanPhonemeUtil.toPronunciation(text);
    }

    public PronunciationPrepareResponse prepareCustomSentence(String text) {
        String trimmed = text.trim();
        String standardPronunciation = convertToStandardPronunciation(trimmed);

        return PronunciationPrepareResponse.builder()
                .sentence(PronunciationPrepareResponse.SentenceResponse.builder()
                        .sentenceId(null)
                        .source("CUSTOM")
                        .originalText(trimmed)
                        .standardPronunciation(standardPronunciation)
                        .translation("Practice with your own sentence.")
                        .categoryLabel("Free practice")
                        .learningPoint("Linking across syllables")
                        .initialScore(0)
                        .characters(buildGuideCharacters(standardPronunciation))
                        .build())
                .build();
    }

    public List<GuideCharacterResponse> buildGuideCharacters(String standardPronunciation) {
        List<GuideCharacterResponse> characters = new ArrayList<>();
        int position = 0;

        for (String character : standardPronunciation.codePoints()
                .mapToObj(Character::toString)
                .filter(value -> !value.isBlank())
                .toList()) {
            List<String> phonemes = KoreanPhonemeUtil.toPhonemeList(character);
            String mouthGuideUrl = firstGuideUrl(phonemes, VideoType.MOUTH);
            String tongueGuideUrl = firstGuideUrl(phonemes, VideoType.TONGUE);
            String guideType = resolveGuideType(mouthGuideUrl, tongueGuideUrl);

            characters.add(GuideCharacterResponse.builder()
                    .position(position)
                    .text(character)
                    .pronunciationText(character)
                    .phonemes(phonemes)
                    .guideType(guideType)
                    .guideStatus("NONE".equals(guideType) ? "MISSING" : "AVAILABLE")
                    .mouthGuideUrl(mouthGuideUrl)
                    .tongueGuideUrl(tongueGuideUrl)
                    .note("Focus on " + guideType.toLowerCase() + " placement")
                    .build());
            position++;
        }

        return characters;
    }

    private String firstGuideUrl(List<String> phonemes, VideoType videoType) {
        return phonemes.stream()
                .map(phoneme -> syllableMappingUtil.getImageUrl(phoneme, videoType))
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);
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
                validFormat = littleEndianUnsignedShort(format, 0) == 1
                        && littleEndianUnsignedShort(format, 2) == 1
                        && littleEndianUnsignedShort(format, 14) == 16;
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
        Path tempFile = null;

        try {
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
                    // Best-effort cleanup only.
                }
            }
        }
    }

    private String resolveReferenceText(Long sentenceId, String text) {
        if (sentenceId != null) {
            RecommendedSentence sentence = requireSentenceRepository()
                    .findBySentenceIdAndActiveTrue(sentenceId)
                    .orElseThrow(() -> new SentenceNotFoundException(sentenceId));
            return sentence.getStandardPronunciation();
        }

        return convertToStandardPronunciation(text.trim());
    }

    private PracticeResultResponse toPracticeResult(String referenceText, AssessmentResult result) {
        int overallScore = toScore(result.getPronunciationScore());
        List<GuideCharacterResponse> guideCharacters = buildGuideCharacters(referenceText);
        boolean characterScoresAvailable = hasReliableCharacterScores(guideCharacters, result);
        List<GuideCharacterResponse> characters = guideCharacters.stream()
                .map(character -> {
                    Integer characterScore = characterScoresAvailable
                            ? toScore(result.getCharacterScores().get(character.getPosition()).accuracyScore())
                            : null;
                    return GuideCharacterResponse.builder()
                        .position(character.getPosition())
                        .text(character.getText())
                        .pronunciationText(character.getPronunciationText())
                        .score(characterScore)
                        .scoreStatus(characterScoresAvailable ? "AVAILABLE" : "UNAVAILABLE")
                        .phonemes(character.getPhonemes())
                        .guideType(character.getGuideType())
                        .guideStatus(character.getGuideStatus())
                        .mouthGuideUrl(character.getMouthGuideUrl())
                        .tongueGuideUrl(character.getTongueGuideUrl())
                        .note(character.getNote())
                        .build();
                })
                .toList();

        List<GuideCharacterResponse> weakCharacters = characterScoresAvailable
                ? characters.stream().filter(character -> character.getScore() < 80).toList()
                : List.of();

        return PracticeResultResponse.builder()
                .overallScore(overallScore)
                .gradeLabel(resolveGradeLabel(overallScore))
                .summary(resolveSummary(overallScore, result.getRecognizedText()))
                .recognizedText(result.getRecognizedText())
                .characterScoreStatus(characterScoresAvailable ? "AVAILABLE" : "UNAVAILABLE")
                .scoreBreakdown(PracticeResultResponse.ScoreBreakdownResponse.builder()
                        .accuracy(toScore(result.getAccuracyScore()))
                        .fluency(toScore(result.getFluencyScore()))
                        .completeness(toScore(result.getCompletenessScore()))
                        .build())
                .characters(characters)
                .weakCharacters(weakCharacters)
                .build();
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
