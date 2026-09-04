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
 *
 * {@code scoreStatus}의 허용 값은 {@link ScoreStatus}에 정의되어 있다.
 * {@code UNAVAILABLE}일 때 {@code score}는 항상 null이며, 이는 0점이 아니라 공급자 token이
 * 기준 단어와 정렬되지 않아 점수를 신뢰할 수 없다는 뜻이다. 클라이언트는 이 경우 숫자를
 * 감추고 음절 가이드만 노출해야 한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeWordResultResponse {
    private int position;
    private String text;

    /** 어절의 로마자 표기다. 한글을 못 읽는 학습자가 대상이라 화면마다 병기한다. */
    private String romanization;
    private Integer score;
    private ScoreStatus scoreStatus;
    private List<GuideCharacterResponse> syllables;
}
