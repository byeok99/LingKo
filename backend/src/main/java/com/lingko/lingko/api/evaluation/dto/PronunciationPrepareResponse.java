package com.lingko.lingko.api.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * HTTP 경계에서 사용하는 Pronunciation Prepare 요청·응답 구조를 정의한다.
 *
 * 영속 엔티티를 직접 노출하지 않고 전송 계약을 독립적으로 유지하기 위해 전용 DTO를 선택했다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PronunciationPrepareResponse {
    private SentenceResponse sentence;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SentenceResponse {
        private Long sentenceId;
        private String source;
        private String originalText;
        private String standardPronunciation;
        private String translation;
        private String categoryLabel;
        private String learningPoint;
        private int initialScore;
        private List<GuideCharacterResponse> characters;
    }
}
