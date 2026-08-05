package com.lingko.lingko.api.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
    private LocalDateTime createdAt;
}
