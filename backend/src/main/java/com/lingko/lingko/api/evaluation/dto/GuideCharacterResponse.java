package com.lingko.lingko.api.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * HTTP 경계에서 사용하는 Guide Character 요청·응답 구조를 정의한다.
 *
 * 영속 엔티티를 직접 노출하지 않고 전송 계약을 독립적으로 유지하기 위해 전용 DTO를 선택했다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideCharacterResponse {
    private int position;
    private String text;
    private String pronunciationText;
    private Integer score;
    private String scoreStatus;
    private List<String> phonemes;
    private String guideType;
    private String guideStatus;
    private String mouthGuideUrl;
    private String tongueGuideUrl;
    private String note;
}
