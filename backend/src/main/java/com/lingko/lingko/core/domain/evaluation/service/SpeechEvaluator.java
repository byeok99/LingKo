package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.domain.evaluation.dto.AssessmentResult;

/**
 * Speech Evaluator 기능의 도메인 경계를 정의한다.
 *
 * 업무 조율이 특정 외부 공급자 구현에 결합되지 않도록 interface를 선택했다.
 */
public interface SpeechEvaluator {
    AssessmentResult evaluate(String audioPath, String referneceText);
}
