package com.lingko.lingko.api.evaluation.dto;

/**
 * 해당 글자에 입·혀 가이드 자료가 준비되어 있는지 나타내는 전송 계약 상태값이다.
 *
 * {@link ScoreStatus}와 값 이름이 겹치지만 의미 축이 다르다. 점수를 믿을 수 있는지와 가이드가
 * 있는지는 서로 독립이며, 실제로 점수가 없어도 가이드는 보여줘야 하는 조합이 정상 동작이다.
 * 두 축을 한 enum으로 합치면 {@code AVAILABLE}이 두 가지를 뜻하게 되므로 타입을 분리했다.
 */
public enum GuideStatus {

    /** 입 또는 혀 가이드 URL이 하나 이상 준비되어 있다. */
    AVAILABLE,

    /** 이 글자에 연결된 가이드 자료가 아직 없다. */
    MISSING
}
