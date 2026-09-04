package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.core.domain.evaluation.entity.Syllable;
import com.lingko.lingko.core.domain.evaluation.repository.SyllableRepository;
import com.lingko.lingko.core.util.SyllableMappingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

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
    private final SyllableRepository syllableRepository;

    @Autowired
    public GuideMediaResolver(
            SyllableMappingUtil syllableMappingUtil,
            VideoGenerator videoGenerator,
            SyllableRepository syllableRepository
    ) {
        this.syllableMappingUtil = syllableMappingUtil;
        this.videoGenerator = videoGenerator;
        this.syllableRepository = syllableRepository;
    }

    public GuideMediaResolver(
            SyllableMappingUtil syllableMappingUtil,
            VideoGenerator videoGenerator
    ) {
        this(syllableMappingUtil, videoGenerator, null);
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
        String persistedVideo = findPersistedVideo(syllable, videoType);
        if (persistedVideo != null) {
            return persistedVideo;
        }

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
            if (generatedUrl == null || generatedUrl.isBlank()) {
                return fallbackUrl;
            }
            saveGeneratedVideo(syllable, videoType, generatedUrl);
            return generatedUrl;
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

    private String findPersistedVideo(String syllable, VideoType videoType) {
        if (syllableRepository == null || syllable == null || syllable.isBlank()) {
            return null;
        }
        return syllableRepository.findById(syllable.trim())
                .map(saved -> videoType == VideoType.MOUTH ? saved.getMouthUrl() : saved.getTongueUrl())
                .filter(this::isVideoUrl)
                .orElse(null);
    }

    private void saveGeneratedVideo(String syllable, VideoType videoType, String generatedUrl) {
        if (syllableRepository == null || !isVideoUrl(generatedUrl)) {
            return;
        }
        try {
            String normalizedSyllable = syllable.trim();
            Syllable current = syllableRepository.findById(normalizedSyllable)
                    .orElseGet(() -> Syllable.builder().syllableChar(normalizedSyllable).build());
            Syllable updated = Syllable.builder()
                    .syllableChar(normalizedSyllable)
                    .mouthUrl(videoType == VideoType.MOUTH ? generatedUrl : current.getMouthUrl())
                    .tongueUrl(videoType == VideoType.TONGUE ? generatedUrl : current.getTongueUrl())
                    .build();
            syllableRepository.save(updated);
        } catch (RuntimeException exception) {
            // S3 영상은 이미 결정적 key로 남아 있으므로 DB metadata 실패가 평가 결과를 이미지로 강등시키지 않게 한다.
            log.warn(
                    "Failed to persist generated guide video: syllable={}, type={}",
                    syllable,
                    videoType,
                    exception
            );
        }
    }

    private boolean isVideoUrl(String url) {
        return url != null && url.toLowerCase(Locale.ROOT).contains(".mp4");
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
