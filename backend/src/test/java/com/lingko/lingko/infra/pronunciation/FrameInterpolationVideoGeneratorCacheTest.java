package com.lingko.lingko.infra.pronunciation;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.infra.storage.ExternalMediaUrlValidator;
import com.lingko.lingko.infra.storage.S3Uploader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 같은 음절·프레임 조합의 생성 영상이 S3에서 재사용되는 계약을 검증한다.
 */
class FrameInterpolationVideoGeneratorCacheTest {

    @Test
    @DisplayName("새 전이 clip은 원본으로 cache하고 최종 영상만 한 번 호환 보정한다")
    void normalizesOnlyTheFinalVideoWhenGeneratingANewSegment() throws Exception {
        ReplicateApiClient replicateApiClient = mock(ReplicateApiClient.class);
        VideoMerger videoMerger = mock(VideoMerger.class);
        S3Uploader s3Uploader = mock(S3Uploader.class);
        ExternalMediaUrlValidator urlValidator = mock(ExternalMediaUrlValidator.class);
        VideoPlaybackNormalizer normalizer = mock(VideoPlaybackNormalizer.class);
        FrameInterpolationVideoGenerator generator = new FrameInterpolationVideoGenerator(
                replicateApiClient,
                videoMerger,
                s3Uploader,
                urlValidator,
                normalizer
        );
        List<List<String>> pairs = List.of(
                List.of("https://guides/tongue/g.png", "https://guides/tongue/a.png")
        );
        when(s3Uploader.findPublicUrl(anyString())).thenReturn(Optional.empty());
        when(replicateApiClient.interpolate(anyString(), anyString()))
                .thenReturn("https://provider/generated.mp4");
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(urlValidator.openConnection("https://provider/generated.mp4")).thenReturn(connection);
        when(normalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(s3Uploader.upload(anyString(), startsWith("videos/tongue/")))
                .thenReturn("https://bucket/videos/tongue/final.mp4");

        generator.generate(pairs, "가", VideoType.TONGUE);

        verify(normalizer, times(1)).normalize(any());
        verify(s3Uploader).upload(anyString(), startsWith("guide-segments/tongue/"));
        verify(s3Uploader).upload(anyString(), startsWith("videos/tongue/"));
        verifyNoInteractions(videoMerger);
    }

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
        when(s3Uploader.findPublicUrl(startsWith("videos/tongue/tongue_phonology-v3_")))
                .thenReturn(Optional.of("https://bucket/videos/tongue/cached.mp4"));

        String result = generator.generate(pairs, "김", VideoType.TONGUE);

        assertThat(result).isEqualTo("https://bucket/videos/tongue/cached.mp4");
        verifyNoInteractions(replicateApiClient, videoMerger, urlValidator);
    }

    @Test
    @DisplayName("음절이 달라도 프레임 시퀀스가 같으면 같은 S3 영상을 조회한다")
    void sharesCachedVideoAcrossSyllablesWithTheSameFrames() {
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
                List.of("https://guides/tongue/g.png", "https://guides/tongue/a.png")
        );
        when(s3Uploader.findPublicUrl(anyString()))
                .thenReturn(Optional.of("https://bucket/videos/tongue/shared.mp4"));

        generator.generate(pairs, "가", VideoType.TONGUE);
        generator.generate(pairs, "까", VideoType.TONGUE);

        var keyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(s3Uploader, times(2)).findPublicUrl(keyCaptor.capture());
        assertThat(keyCaptor.getAllValues()).containsOnly(keyCaptor.getAllValues().get(0));
        verifyNoInteractions(replicateApiClient, videoMerger, urlValidator);
    }

    @Test
    @DisplayName("완성 영상 cache miss여도 기존 전이 clip이 있으면 Replicate를 호출하지 않는다")
    void reusesCachedTransitionSegment() throws Exception {
        ReplicateApiClient replicateApiClient = mock(ReplicateApiClient.class);
        VideoMerger videoMerger = mock(VideoMerger.class);
        S3Uploader s3Uploader = mock(S3Uploader.class);
        ExternalMediaUrlValidator urlValidator = mock(ExternalMediaUrlValidator.class);
        VideoPlaybackNormalizer normalizer = mock(VideoPlaybackNormalizer.class);
        FrameInterpolationVideoGenerator generator = new FrameInterpolationVideoGenerator(
                replicateApiClient,
                videoMerger,
                s3Uploader,
                urlValidator,
                normalizer
        );
        List<List<String>> pairs = List.of(
                List.of("https://guides/tongue/g.png", "https://guides/tongue/a.png")
        );
        when(s3Uploader.findPublicUrl(argThat(key -> key != null && key.startsWith("videos/tongue/"))))
                .thenReturn(Optional.empty());
        when(s3Uploader.findPublicUrl(argThat(key -> key != null && key.startsWith("guide-segments/tongue/"))))
                .thenReturn(Optional.of("https://lingko.s3.ap-northeast-2.amazonaws.com/guide-segments/tongue/cached.mp4"));
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(urlValidator.openConnection(anyString())).thenReturn(connection);
        when(normalizer.normalize(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(s3Uploader.upload(anyString(), startsWith("videos/tongue/")))
                .thenReturn("https://bucket/videos/tongue/final.mp4");

        String result = generator.generate(pairs, "가", VideoType.TONGUE);

        assertThat(result).isEqualTo("https://bucket/videos/tongue/final.mp4");
        verify(replicateApiClient, never()).interpolate(anyString(), anyString());
        verify(s3Uploader, never()).upload(anyString(), startsWith("guide-segments/"));
        verifyNoInteractions(videoMerger);
    }
}
