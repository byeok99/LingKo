package com.lingko.lingko.infra.pronunciation;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import com.lingko.lingko.infra.storage.ExternalMediaUrlValidator;
import com.lingko.lingko.infra.storage.S3Uploader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class FrameInterpolationVideoGeneratorTest {

    @Test
    @DisplayName("urlPairs가 null이면 VideoGenerationException으로 변환한다")
    void generateRejectsNullUrlPairs() {
        FrameInterpolationVideoGenerator generator = new FrameInterpolationVideoGenerator(
                mock(ReplicateApiClient.class),
                mock(VideoMerger.class),
                mock(S3Uploader.class),
                mock(ExternalMediaUrlValidator.class)
        );

        assertThatThrownBy(() -> generator.generate(null, "가", VideoType.MOUTH))
                .isInstanceOf(VideoGenerationException.class)
                .hasMessageContaining("영상 생성 실패");
    }
}
