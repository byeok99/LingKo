package com.lingko.lingko.api.evaluation.dto;

/**
 * 평가 응답에 실린 점수를 사용자에게 보여도 되는지 나타내는 전송 계약 상태값이다.
 *
 * 공급자 token이 기준 문장과 정렬되지 않으면 점수를 "0점"으로 내리는 대신 신뢰 불가로 표시해야
 * 하는데, nullable 숫자 하나로는 "0점"과 "모름"을 구분할 수 없어 상태값을 함께 둔다.
 * enum으로 정의해 서버 안에서는 오타와 누락된 분기를 컴파일 시점에 막고, JSON에는 Jackson 기본
 * 직렬화인 {@link Enum#name()} 그대로 나가 기존 wire format을 유지한다.
 */
public enum ScoreStatus {

    /** 점수 필드가 실제 측정값이며 사용자에게 노출해도 된다. */
    AVAILABLE,

    /** 점수를 신뢰할 수 없어 숫자를 노출하면 안 된다. 이때 점수 필드는 항상 null이다. */
    UNAVAILABLE;

    /**
     * 점수 유무만으로 상태를 정하는 저장·복원 경로에서 두 값의 대응을 한곳에 모은다.
     *
     * 호출부마다 삼항 연산자를 반복하면 null을 AVAILABLE로 잘못 매핑하는 실수가 생길 수 있어
     * 변환 규칙 자체를 이 타입이 소유한다.
     */
    public static ScoreStatus ofNullableScore(Integer score) {
        return score == null ? UNAVAILABLE : AVAILABLE;
    }

    public boolean isAvailable() {
        return this == AVAILABLE;
    }
}
