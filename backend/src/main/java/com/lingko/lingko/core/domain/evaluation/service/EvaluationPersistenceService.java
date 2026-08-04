package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.api.evaluation.dto.GuideCharacterResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeWordResultResponse;
import com.lingko.lingko.api.evaluation.dto.ScoreStatus;
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
        if (hasReliableWordScores(result, words)) {
            for (PracticeWordResultResponse word : words) {
                evaluationLog.addWord(toWordResult(word));
                for (GuideCharacterResponse syllable : safeCharacters(word.getSyllables())) {
                    evaluationLog.addSyllable(toSyllableResult(syllable, word.getPosition()));
                }
            }
        } else {
            for (GuideCharacterResponse character : safeCharacters(result.getCharacters())) {
                evaluationLog.addSyllable(toSyllableResult(character, null));
            }
        }

        return evaluationLogRepository.saveAndFlush(evaluationLog);
    }

    /**
     * 점수를 신뢰할 수 있을 때만 단어 행을 남겨 "단어 행이 있다 = 점수가 신뢰 가능했다"를 불변식으로 만든다.
     *
     * 점수가 없는 단어 행이 가지는 정보는 공백으로 자른 텍스트뿐이고 이는 이미 저장하는
     * standard_pronunciation에서 파생할 수 있다. 조회 계층이 그 복원을 담당하므로 같은 정보를
     * 두 벌 저장하지 않는다. 단어 경계를 판단하지 못한 결과가 문장 전체를 한 단어처럼 기록해
     * word_text 길이를 넘기는 것도 이 조건으로 함께 막는다.
     */
    private boolean hasReliableWordScores(
            PracticeResultResponse result,
            List<PracticeWordResultResponse> words
    ) {
        return !words.isEmpty()
                && result.getWordScoreStatus() == ScoreStatus.AVAILABLE
                && words.stream().allMatch(word -> word.getScore() != null);
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
