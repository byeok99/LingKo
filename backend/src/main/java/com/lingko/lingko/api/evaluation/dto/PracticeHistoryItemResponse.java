package com.lingko.lingko.api.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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
    private String recognizedText;
    private int overallScore;
    private String gradeLabel;
    private String summary;
    private PracticeResultResponse.ScoreBreakdownResponse scoreBreakdown;
    private List<PracticeHistoryCharacterResponse> characters;
    private LocalDateTime createdAt;
}
