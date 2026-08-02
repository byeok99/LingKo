package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import com.lingko.lingko.core.util.SyllableMappingUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 음절의 프레임 수에 따라 평가 가이드가 영상 또는 정적 이미지로 결정되는 계약을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class GuideMediaResolverTest {

    @Mock
    private SyllableMappingUtil syllableMappingUtil;
    @Mock
    private VideoGenerator videoGenerator;

    private GuideMediaResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new GuideMediaResolver(syllableMappingUtil, videoGenerator);
    }

    @Test
    @DisplayName("김은 초중종성 프레임 이동을 입과 혀 영상으로 반환한다")
    void resolvesKimTransitionsAsVideos() {
        List<String> phonemes = List.of("ㄱ", "ㅣ", "ㅁ");
        List<List<String>> mouthPairs = List.of(List.of("mouth-i.png", "mouth-m.png"));
        List<List<String>> tonguePairs = List.of(
                List.of("tongue-g.png", "tongue-i.png"),
                List.of("tongue-i.png", "tongue-m.png")
        );
        when(syllableMappingUtil.createFramePairs(phonemes, VideoType.MOUTH))
                .thenReturn(mouthPairs);
        when(syllableMappingUtil.createFramePairs(phonemes, VideoType.TONGUE))
                .thenReturn(tonguePairs);
        when(videoGenerator.generate(mouthPairs, "김", VideoType.MOUTH))
                .thenReturn("https://guides/videos/mouth-kim.mp4");
        when(videoGenerator.generate(tonguePairs, "김", VideoType.TONGUE))
                .thenReturn("https://guides/videos/tongue-kim.mp4");

        assertThat(resolver.resolveForEvaluation("김", phonemes, VideoType.MOUTH))
                .endsWith("mouth-kim.mp4");
        assertThat(resolver.resolveForEvaluation("김", phonemes, VideoType.TONGUE))
                .endsWith("tongue-kim.mp4");
    }

    @Test
    @DisplayName("프레임이 하나뿐이면 외부 영상 생성 없이 이미지를 유지한다")
    void keepsSingleFrameAsStaticImage() {
        List<String> phonemes = List.of("ㅏ");
        when(syllableMappingUtil.createFramePairs(phonemes, VideoType.MOUTH))
                .thenReturn(List.of(List.of("mouth-a.png")));

        assertThat(resolver.resolveForEvaluation("아", phonemes, VideoType.MOUTH))
                .isEqualTo("mouth-a.png");
        verify(videoGenerator, never()).generate(
                List.of(List.of("mouth-a.png")),
                "아",
                VideoType.MOUTH
        );
    }

    @Test
    @DisplayName("영상 생성 실패 시 평가 결과를 막지 않고 첫 이미지로 대체한다")
    void fallsBackToFirstFrameWhenGenerationFails() {
        List<String> phonemes = List.of("ㄱ", "ㅣ", "ㅁ");
        List<List<String>> pairs = List.of(
                List.of("tongue-g.png", "tongue-i.png"),
                List.of("tongue-i.png", "tongue-m.png")
        );
        when(syllableMappingUtil.createFramePairs(phonemes, VideoType.TONGUE))
                .thenReturn(pairs);
        when(videoGenerator.generate(pairs, "김", VideoType.TONGUE))
                .thenThrow(new VideoGenerationException("generation failed"));

        assertThat(resolver.resolveForEvaluation("김", phonemes, VideoType.TONGUE))
                .isEqualTo("tongue-g.png");
    }
}
