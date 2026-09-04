package com.lingko.lingko.api.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 저장된 단어 점수와 하위 음절 가이드를 Review 응답에 제공한다.
 *
 * {@code scoreStatus}의 허용 값과 의미는 {@link PracticeWordResultResponse}와 동일하다.
 * 단어 snapshot이 없는 V15 이전 기록은 공백 기준으로 단어를 복원하되 점수를 복구할 수 없으므로
 * 항상 {@code UNAVAILABLE}로 응답한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeHistoryWordResponse {
    private int position;
    private String text;
    private String romanization;
    private Integer score;
    private ScoreStatus scoreStatus;
    private List<PracticeHistoryCharacterResponse> syllables;
}
