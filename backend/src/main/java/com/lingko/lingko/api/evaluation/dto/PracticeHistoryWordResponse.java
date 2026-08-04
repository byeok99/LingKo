package com.lingko.lingko.api.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 저장된 단어 점수와 하위 음절 가이드를 Review 응답에 제공한다.

 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeHistoryWordResponse {
    private int position;
    private String text;
    private Integer score;
    private String scoreStatus;
    private List<PracticeHistoryCharacterResponse> syllables;
}
