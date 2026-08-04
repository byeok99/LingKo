package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.api.evaluation.dto.GuideCharacterResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeWordResultResponse;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationSyllable;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationWord;
import com.lingko.lingko.core.domain.evaluation.entity.Syllable;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationLogRepository;
import com.lingko.lingko.core.domain.evaluation.repository.SyllableRepository;
import com.lingko.lingko.core.domain.user.entity.User;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Evaluation Persistence 업무 규칙을 조율한다.
 *
 * 컨트롤러와 외부 어댑터가 정책을 소유하지 않도록 도메인 서비스에 조율을 집중했다.
 */
@Service
@RequiredArgsConstructor
public class EvaluationPersistenceService {

    private final EvaluationLogRepository evaluationLogRepository;
    private final SyllableRepository syllableRepository;

    @Transactional
    public EvaluationLog saveResult(SaveEvaluationResultCommand command) {
        // 부분 평가 결과가 노출되지 않도록 log와 모든 문자 child를 하나의 aggregate로 저장한다.
        validate(command);

        PracticeResultResponse result = command.result();
        PracticeResultResponse.ScoreBreakdownResponse breakdown = result.getScoreBreakdown();
        EvaluationLog evaluationLog = EvaluationLog.builder()
                .user(command.user())
                .originalWord(command.originalText().trim())
                .score(result.getOverallScore())
                .source(command.source())
                .sentenceId(command.sentenceId())
                .standardPronunciation(command.standardPronunciation().trim())
                .recognizedText(blankToNull(result.getRecognizedText()))
                .accuracyScore(scoreToDecimal(breakdown == null ? null : breakdown.getAccuracy()))
                .fluencyScore(scoreToDecimal(breakdown == null ? null : breakdown.getFluency()))
                .completenessScore(scoreToDecimal(breakdown == null ? null : breakdown.getCompleteness()))
                .pronunciationScore(scoreToDecimal(result.getOverallScore()))
                .audioUrl(blankToNull(command.audioUrl()))
                .build();

        List<PracticeWordResultResponse> words = safeWords(result.getWords());
        if (words.isEmpty()) {
            for (GuideCharacterResponse character : safeCharacters(result.getCharacters())) {
                evaluationLog.addSyllable(toSyllableResult(character, null));
            }
        } else {
            for (PracticeWordResultResponse word : words) {
                evaluationLog.addWord(toWordResult(word));
                for (GuideCharacterResponse syllable : safeCharacters(word.getSyllables())) {
                    evaluationLog.addSyllable(toSyllableResult(syllable, word.getPosition()));
                }
            }
        }

        return evaluationLogRepository.saveAndFlush(evaluationLog);
    }

    private EvaluationWord toWordResult(PracticeWordResultResponse word) {
        return EvaluationWord.builder()
                .positionNo(word.getPosition())
                .wordText(requireText(word.getText(), "word text"))
                .score(word.getScore())
                .build();
    }

    private EvaluationSyllable toSyllableResult(
            GuideCharacterResponse character,
            Integer wordPosition
    ) {
        String text = requireText(character.getText(), "character text");
        // 표준 음절 metadata를 재사용하고 새 문자가 처음 등장할 때만 생성한다.
        Syllable syllable = syllableRepository.findById(text)
                .orElseGet(() -> syllableRepository.save(Syllable.builder()
                        .syllableChar(text)
                        .mouthUrl(blankToNull(character.getMouthGuideUrl()))
                        .tongueUrl(blankToNull(character.getTongueGuideUrl()))
                        .build()));

        return EvaluationSyllable.builder()
                .syllable(syllable)
                .score(character.getScore())
                .positionNo(character.getPosition())
                .wordPosition(wordPosition)
                .feedback(blankToNull(character.getNote()))
                .mouthGuideUrl(blankToNull(character.getMouthGuideUrl()))
                .tongueGuideUrl(blankToNull(character.getTongueGuideUrl()))
                .build();
    }

    private void validate(SaveEvaluationResultCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }
        if (command.user() == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        if (command.source() == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        requireText(command.originalText(), "originalText");
        requireText(command.standardPronunciation(), "standardPronunciation");
        if (command.result() == null) {
            throw new IllegalArgumentException("result must not be null");
        }
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private List<GuideCharacterResponse> safeCharacters(List<GuideCharacterResponse> characters) {
        return characters == null ? List.of() : characters;
    }

    private List<PracticeWordResultResponse> safeWords(List<PracticeWordResultResponse> words) {
        return words == null ? List.of() : words;
    }

    private BigDecimal scoreToDecimal(Integer score) {
        return score == null ? null : BigDecimal.valueOf(score);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Builder
    public record SaveEvaluationResultCommand(
            User user,
            EvaluationLog.PracticeSource source,
            Long sentenceId,
            String originalText,
            String standardPronunciation,
            String audioUrl,
            PracticeResultResponse result
    ) {
    }
}
