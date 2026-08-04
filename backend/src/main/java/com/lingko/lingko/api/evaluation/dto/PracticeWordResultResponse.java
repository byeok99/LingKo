package com.lingko.lingko.api.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 신뢰 가능한 단어 점수와 해당 단어의 음절 가이드를 하나의 API 단위로 제공한다.
 *
 * 음절에 단어 점수를 복제하지 않고 상위 단어에서만 점수 의미를 소유한다.

 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeWordResultResponse {
    private int position;
    private String text;
    private Integer score;
    private String scoreStatus;
    private List<GuideCharacterResponse> syllables;
}
