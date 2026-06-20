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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class EvaluationService {

    private final SyllableMappingUtil syllableMappingUtil;
    private final SpeechEvaluator speechEvaluator;
    private final RecommendedSentenceRepository sentenceRepository;

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
                .practiceToken("prep_" + UUID.randomUUID())
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
        String filename = audio.getOriginalFilename();
        String contentType = audio.getContentType();
        boolean wavName = filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".wav");
        boolean wavType = contentType == null
                || contentType.equalsIgnoreCase("audio/wav")
                || contentType.equalsIgnoreCase("audio/x-wav")
                || contentType.equalsIgnoreCase("audio/vnd.wave")
                || contentType.equalsIgnoreCase("application/octet-stream");

        return wavName && wavType;
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

            throw new VideoGenerationException("Speech evaluation failed");
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
        List<GuideCharacterResponse> characters = buildGuideCharacters(referenceText).stream()
                .map(character -> GuideCharacterResponse.builder()
                        .position(character.getPosition())
                        .text(character.getText())
                        .pronunciationText(character.getPronunciationText())
                        .score(overallScore)
                        .phonemes(character.getPhonemes())
                        .guideType(character.getGuideType())
                        .guideStatus(character.getGuideStatus())
                        .mouthGuideUrl(character.getMouthGuideUrl())
                        .tongueGuideUrl(character.getTongueGuideUrl())
                        .note(character.getNote())
                        .build())
                .toList();

        return PracticeResultResponse.builder()
                .overallScore(overallScore)
                .gradeLabel(resolveGradeLabel(overallScore))
                .summary(resolveSummary(overallScore, result.getRecognizedText()))
                .scoreBreakdown(PracticeResultResponse.ScoreBreakdownResponse.builder()
                        .accuracy(toScore(result.getAccuracyScore()))
                        .fluency(toScore(result.getFluencyScore()))
                        .completeness(toScore(result.getCompletenessScore()))
                        .build())
                .characters(characters)
                .weakCharacters(overallScore < 80 ? characters : List.of())
                .build();
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
        if (score >= 90) {
            return "Clear pronunciation. Keep the same rhythm and articulation.";
        }

        if (score >= 75) {
            return "Good pronunciation. Review the highlighted sounds and try once more.";
        }

        return "Pronunciation needs more practice. Focus on the guide items before retrying.";
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
