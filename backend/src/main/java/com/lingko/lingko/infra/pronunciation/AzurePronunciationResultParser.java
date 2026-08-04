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
