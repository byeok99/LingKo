package com.lingko.lingko.api.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeResultResponse {
    private int overallScore;
    private String gradeLabel;
    private String summary;
    private ScoreBreakdownResponse scoreBreakdown;
    private List<GuideCharacterResponse> weakCharacters;
    private List<GuideCharacterResponse> characters;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScoreBreakdownResponse {
        private int accuracy;
        private int fluency;
        private int completeness;
    }
}
