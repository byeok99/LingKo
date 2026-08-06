package com.lingko.lingko.infra.pronunciation;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import com.lingko.lingko.infra.storage.ExternalMediaUrlValidator;
import com.lingko.lingko.infra.storage.S3Uploader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Frame Interpolation Video Generator Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
class FrameInterpolationVideoGeneratorTest {

    @Test
    @DisplayName("urlPairs가 null이면 VideoGenerationException으로 변환한다")
    void generateRejectsNullUrlPairs() {
        FrameInterpolationVideoGenerator generator = new FrameInterpolationVideoGenerator(
                mock(ReplicateApiClient.class),
                mock(VideoMerger.class),
                mock(S3Uploader.class),
                mock(ExternalMediaUrlValidator.class),
                mock(VideoPlaybackNormalizer.class)
        );

        assertThatThrownBy(() -> generator.generate(null, "가", VideoType.MOUTH))
                .isInstanceOf(VideoGenerationException.class)
                .hasMessageContaining("영상 생성 실패");
    }
}
