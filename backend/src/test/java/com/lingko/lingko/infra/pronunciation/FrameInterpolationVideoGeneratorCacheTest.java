package com.lingko.lingko.infra.pronunciation;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.infra.storage.ExternalMediaUrlValidator;
import com.lingko.lingko.infra.storage.S3Uploader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 같은 음절·프레임 조합의 생성 영상이 S3에서 재사용되는 계약을 검증한다.
 */
class FrameInterpolationVideoGeneratorCacheTest {

    @Test
    @DisplayName("동일한 김 프레임 영상이 있으면 외부 보간을 다시 호출하지 않는다")
    void reusesCachedKimVideo() {
        ReplicateApiClient replicateApiClient = mock(ReplicateApiClient.class);
        VideoMerger videoMerger = mock(VideoMerger.class);
        S3Uploader s3Uploader = mock(S3Uploader.class);
        ExternalMediaUrlValidator urlValidator = mock(ExternalMediaUrlValidator.class);
        FrameInterpolationVideoGenerator generator = new FrameInterpolationVideoGenerator(
                replicateApiClient,
                videoMerger,
                s3Uploader,
                urlValidator,
                mock(VideoPlaybackNormalizer.class)
        );
        List<List<String>> pairs = List.of(
                List.of("https://guides/tongue/g.png", "https://guides/tongue/i.png"),
                List.of("https://guides/tongue/i.png", "https://guides/tongue/m.png")
        );
        when(s3Uploader.findPublicUrl(startsWith("videos/tongue/")))
                .thenReturn(Optional.of("https://bucket/videos/tongue/cached.mp4"));

        String result = generator.generate(pairs, "김", VideoType.TONGUE);

        assertThat(result).isEqualTo("https://bucket/videos/tongue/cached.mp4");
        verifyNoInteractions(replicateApiClient, videoMerger, urlValidator);
    }
}
