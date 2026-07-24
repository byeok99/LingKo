package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;

import java.util.List;

/**
 * Video Generator 기능의 도메인 경계를 정의한다.
 *
 * 업무 조율이 특정 외부 공급자 구현에 결합되지 않도록 interface를 선택했다.
 */
public interface VideoGenerator {
    /**
     * 영상 생성 로직
     * @param urlPairs [["start.png", "end.png"]]
     * @param syllable 음절
     * @param type 영상 타입
     * @return S3 URL
     */
    String generate(List<List<String>> urlPairs, String syllable, VideoType type);

}
