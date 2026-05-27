package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;

import java.util.List;

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
