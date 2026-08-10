package com.lingko.lingko.core.domain.evaluation.exception;

/** 인증된 일반 사용자가 내부 가이드 생성 기능에 접근했음을 나타낸다. */
public class GuideJobAccessDeniedException extends RuntimeException {

    public GuideJobAccessDeniedException() {
        super("Guide generation is restricted to an internal service");
    }
}
