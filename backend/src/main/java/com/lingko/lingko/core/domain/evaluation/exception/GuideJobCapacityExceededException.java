package com.lingko.lingko.core.domain.evaluation.exception;

/** 이미 허용된 최대 가이드 생성 작업이 실행 중임을 나타낸다. */
public class GuideJobCapacityExceededException extends RuntimeException {

    public GuideJobCapacityExceededException() {
        super("Guide generation capacity is exhausted");
    }
}
