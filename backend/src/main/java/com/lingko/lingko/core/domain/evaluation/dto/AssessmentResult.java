package com.lingko.lingko.core.domain.evaluation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AssessmentResult {
    private Double accuracyScore;
    private Double fluencyScore;
    private Double completenessScore;
    private Double pronunciationScore;
    private String recognizedText;
    private boolean characterScoresAvailable;
    private List<CharacterScore> characterScores;

    public record CharacterScore(int position, String text, Double accuracyScore) {
    }
}
