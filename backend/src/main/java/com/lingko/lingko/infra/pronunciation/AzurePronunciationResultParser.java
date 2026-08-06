package com.lingko.lingko.infra.pronunciation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.core.domain.evaluation.dto.AssessmentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Azure detailed JSON에서 기준 문장과 정확히 정렬된 단어 점수만 추출한다.
 *
 * 한국어 tokenization 차이를 위치로 추측하지 않고 개수·텍스트·점수가 모두 일치할 때만 신뢰한다.
 */
@Slf4j
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
     *
     * <p>버릴 때는 반드시 이유를 남긴다. 이 경로는 호출자에게 빈 목록만 돌려주므로 어긋난
     * 지점을 남기지 않으면 화면에서 단어 점수가 사라진 것만 보이고 원인을 되짚을 방법이 없다.
     * 단어별 피드백이 통째로 사라지는 것은 사용자 흐름을 막지는 않아도 기능 저하이므로 WARN이다.
     *
     * <p>남기는 값은 개수·위치와 어긋난 토큰 한 쌍뿐이다. 기준 문장 전체나 인식된 문장은
     * 남기지 않는다. 토큰은 이미 {@code evaluation_log.standard_pronunciation}에 저장되는
     * 자료의 일부라 새로 노출되는 정보가 아니면서, 텍스트 불일치는 토큰을 봐야만 진단된다.
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
            if (!wordsNode.isArray()) {
                log.warn("Azure word scores dropped: reason=missing-words-array, expectedWords={}",
                        expectedWords.size());
                return List.of();
            }
            if (wordsNode.size() != expectedWords.size()) {
                log.warn("Azure word scores dropped: reason=count-mismatch, expectedWords={}, actualWords={}",
                        expectedWords.size(), wordsNode.size());
                return List.of();
            }

            List<AssessmentResult.WordScore> scores = new ArrayList<>(expectedWords.size());
            for (int position = 0; position < expectedWords.size(); position++) {
                JsonNode wordNode = wordsNode.get(position);
                String expected = expectedWords.get(position);
                String actual = normalizeComparable(wordNode.path("Word").asText(""));
                JsonNode scoreNode = wordNode.path("PronunciationAssessment").path("AccuracyScore");
                if (!normalizeComparable(expected).equals(actual)) {
                    log.warn("Azure word scores dropped: reason=text-mismatch, position={}, expected='{}', actual='{}'",
                            position, expected, actual);
                    return List.of();
                }
                if (!scoreNode.isNumber()) {
                    log.warn("Azure word scores dropped: reason=missing-score, position={}", position);
                    return List.of();
                }
                scores.add(new AssessmentResult.WordScore(position, expected, scoreNode.doubleValue()));
            }
            return List.copyOf(scores);
        } catch (JsonProcessingException exception) {
            log.warn("Azure word scores dropped: reason=malformed-json", exception);
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
