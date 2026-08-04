package com.lingko.lingko.core.domain.evaluation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 도메인 서비스가 사용하는 공급자 독립적인 Assessment Result 값을 전달한다.
 *
 * 업무 의미를 외부 API의 응답 형식과 분리하기 위해 내부 모델을 둔다.
 */
@Getter
@Builder
public class AssessmentResult {
    private Double accuracyScore;
    private Double fluencyScore;
    private Double completenessScore;
    private Double pronunciationScore;
    private String recognizedText;
    private boolean characterScoresAvailable;
    private List<CharacterScore> characterScores;
    private boolean wordScoresAvailable;
    private List<WordScore> wordScores;

    public record CharacterScore(int position, String text, Double accuracyScore) {
    }

    /** 기준 문장의 공백 단위와 공급자 token이 일치한 단어 점수다. */
    public record WordScore(int position, String text, Double accuracyScore) {
    }
}
