package com.lingko.lingko.infra.pronunciation;

/**
 * Azure 발음 평가가 설정된 시간 안에 결과를 돌려주지 않았음을 나타낸다.
 *
 * <p>일시적인 외부 지연으로 보고 평가 작업은 일반 실패와 같이 최대 시도 횟수까지 재시도한다
 * (작업 {@code errorCode}는 {@code EVALUATION_FAILED}). 별도 타입으로 둔 이유는 관측 지표의 outcome을
 * {@code timeout}으로 구분해, 공급자 지연과 인식 실패·인증 오류를 운영에서 따로 볼 수 있게 하기 위해서다.</p>
 */
public class SpeechEvaluationTimeoutException extends IllegalStateException {

    public SpeechEvaluationTimeoutException(long timeoutSeconds, Throwable cause) {
        super("발음 평가가 " + timeoutSeconds + "초 안에 끝나지 않았습니다", cause);
    }
}
