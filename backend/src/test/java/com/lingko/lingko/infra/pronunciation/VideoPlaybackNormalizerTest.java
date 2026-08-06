package com.lingko.lingko.infra.pronunciation;

import com.lingko.lingko.core.config.FfmpegSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 호환 보정이 실패해도 이미 만들어진 가이드 영상을 잃지 않음을 검증한다.
 *
 * 이 단계는 생성이 아니라 보정이므로, 여기서 예외를 던지면 Replicate 호출까지 끝난 결과가
 * 통째로 버려진다. 재생되지 않을지언정 영상이 있는 편이 아무것도 없는 것보다 낫다.
 */
class VideoPlaybackNormalizerTest {

    @Test
    @DisplayName("FFmpeg 실행에 실패하면 예외 대신 원본 경로를 그대로 돌려준다")
    void fallsBackToSourceWhenFfmpegFails(@TempDir Path tempDir) throws Exception {
        FfmpegSettings settings = mock(FfmpegSettings.class);
        // 존재하지 않는 실행 파일로 실패를 강제한다.
        when(settings.getPath()).thenReturn(tempDir.resolve("no-such-ffmpeg").toString());
        Path source = Files.writeString(tempDir.resolve("guide.mp4"), "not-a-real-video");

        Path result = new VideoPlaybackNormalizer(settings).normalize(source);

        assertThat(result).isEqualTo(source);
        // 실패한 보정의 부산물이 남아 임시 파일 정리 대상에서 새면 안 된다.
        assertThat(tempDir.resolve("normalized_guide.mp4")).doesNotExist();
    }
}
