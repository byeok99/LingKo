package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.core.util.SyllableMappingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 음절별 자모 프레임을 준비용 정적 이미지 또는 평가용 전환 영상으로 해석한다.
 *
 * 입력 중 자동 준비에서는 외부 생성 비용을 발생시키지 않고, 비동기 평가 결과를 조립할 때만
 * 다중 프레임을 영상으로 생성한다. 영상 생성 실패는 평가 전체 실패로 확대하지 않고 첫 프레임으로 대체한다.
 */
@Service
@Slf4j
public class GuideMediaResolver {

    private final SyllableMappingUtil syllableMappingUtil;
    private final VideoGenerator videoGenerator;

    public GuideMediaResolver(
            SyllableMappingUtil syllableMappingUtil,
            VideoGenerator videoGenerator
    ) {
        this.syllableMappingUtil = syllableMappingUtil;
        this.videoGenerator = videoGenerator;
    }

    public String resolveStatic(List<String> phonemes, VideoType videoType) {
        return phonemes.stream()
                .map(phoneme -> syllableMappingUtil.getImageUrl(phoneme, videoType))
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);
    }

    public String resolveForEvaluation(
            String syllable,
            List<String> phonemes,
            VideoType videoType
    ) {
        List<List<String>> framePairs = syllableMappingUtil.createFramePairs(
                phonemes,
                videoType
        );
        String fallbackUrl = firstFrame(framePairs);
        if (fallbackUrl == null || isStaticFrame(framePairs) || videoGenerator == null) {
            return fallbackUrl;
        }

        try {
            String generatedUrl = videoGenerator.generate(framePairs, syllable, videoType);
            return generatedUrl == null || generatedUrl.isBlank()
                    ? fallbackUrl
                    : generatedUrl;
        } catch (RuntimeException exception) {
            // 외부 영상 공급자 장애가 이미 완료된 발음 평가 결과까지 무효화하지 않도록 정적 프레임으로 강등한다.
            log.warn(
                    "Guide video generation failed; using static fallback: syllable={}, type={}",
                    syllable,
                    videoType,
                    exception
            );
            return fallbackUrl;
        }
    }

    private boolean isStaticFrame(List<List<String>> framePairs) {
        return framePairs.size() == 1 && framePairs.get(0).size() == 1;
    }

    private String firstFrame(List<List<String>> framePairs) {
        if (framePairs == null || framePairs.isEmpty()) {
            return null;
        }

        return framePairs.stream()
                .flatMap(List::stream)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);
    }
}
