package com.lingko.lingko.api.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * HTTP 경계에서 사용하는 Practice History Item 요청·응답 구조를 정의한다.
 *
 * 영속 엔티티를 직접 노출하지 않고 전송 계약을 독립적으로 유지하기 위해 전용 DTO를 선택했다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeHistoryItemResponse {
    private Long evaluationLogId;
    private Long sentenceId;
    private String source;
    private String originalText;
    private String standardPronunciation;
    /** 저장된 표준 발음에서 조회 시점에 파생한 학습자용 로마자 가이드다. */
    private String romanizedPronunciation;
    private String recognizedText;
    private int overallScore;
    private String gradeLabel;
    private String summary;
    private PracticeResultResponse.ScoreBreakdownResponse scoreBreakdown;
    private List<PracticeHistoryCharacterResponse> characters;
    private List<PracticeHistoryWordResponse> words;
    /** 기록 시점(UTC). 응답에는 "2026-06-26T06:00:00Z"처럼 시간대가 붙어 앱이 기기 시간으로 바꿔 보여 준다. */
    private Instant createdAt;
}
