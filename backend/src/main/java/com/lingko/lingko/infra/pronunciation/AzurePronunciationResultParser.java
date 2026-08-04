package com.lingko.lingko.infra.pronunciation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.core.domain.evaluation.dto.AssessmentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Azure detailed JSON에서 기준 문장과 정확히 정렬된 단어 점수만 추출한다.
 *
 * 한국어 tokenization 차이를 위치로 추측하지 않고 개수·텍스트·점수가 모두 일치할 때만 신뢰한다.
 */
@Component
@RequiredArgsConstructor
public class AzurePronunciationResultParser {

    private final ObjectMapper objectMapper;

    /**
     * 기준 문장과 완전히 정렬된 단어 점수만 반환하고, 하나라도 어긋나면 빈 목록을 반환한다.
     *
     * 부분 성공을 허용하면 어긋난 위치의 점수가 다른 단어에 붙어 사용자에게 잘못된 피드백이
     * 노출되므로, 전부 신뢰하거나 전부 포기하는 all-or-nothing 계약을 선택했다. 빈 목록은
     * 호출자에게 "점수 없음"이 아니라 "이번 응답의 단어 점수를 쓰지 말 것"을 의미한다.
     */
    public List<AssessmentResult.WordScore> parseReliableWordScores(
            String rawJson,
            String referenceText
    ) {
        if (rawJson == null || rawJson.isBlank() || referenceText == null || referenceText.isBlank()) {
            return List.of();
        }

        try {
            List<String> expectedWords = splitWords(referenceText);
            JsonNode wordsNode = objectMapper.readTree(rawJson).path("NBest").path(0).path("Words");
            if (!wordsNode.isArray() || wordsNode.size() != expectedWords.size()) {
                return List.of();
            }

            List<AssessmentResult.WordScore> scores = new ArrayList<>(expectedWords.size());
            for (int position = 0; position < expectedWords.size(); position++) {
                JsonNode wordNode = wordsNode.get(position);
                String expected = expectedWords.get(position);
                String actual = normalizeComparable(wordNode.path("Word").asText(""));
                JsonNode scoreNode = wordNode.path("PronunciationAssessment").path("AccuracyScore");
                if (!normalizeComparable(expected).equals(actual) || !scoreNode.isNumber()) {
                    return List.of();
                }
                scores.add(new AssessmentResult.WordScore(position, expected, scoreNode.doubleValue()));
            }
            return List.copyOf(scores);
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private List<String> splitWords(String value) {
        return List.of(value.trim().split("\\s+"));
    }

    private String normalizeComparable(String value) {
        return value.replaceAll("[\\p{P}\\p{S}\\s]+", "");
    }
}
