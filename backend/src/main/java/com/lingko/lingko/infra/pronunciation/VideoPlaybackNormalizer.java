package com.lingko.lingko.infra.pronunciation;

import com.lingko.lingko.core.config.FfmpegSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 가이드 영상을 모바일이 실제로 디코딩할 수 있는 형식으로 맞춘다.
 *
 * <p><b>왜 필요한가</b>: Replicate 보간 결과는 원본 도해 PNG의 크기를 그대로 따르는데,
 * 그 크기에 홀수 변이 섞여 있다(예: 309x157). {@code yuv420p}는 크로마를 가로·세로로 2배씩
 * 서브샘플링하므로 짝수 해상도를 요구하고, 변이 홀수면 인코더가 {@code yuv444p}
 * (H.264 High 4:4:4 Predictive)로 떨어진다. Apple VideoToolbox는 4:4:4를 디코딩하지 못한다.
 * 이때 AVPlayer는 오류를 내지 않고 ready 상태까지 정상 진행한 뒤 프레임만 내놓지 않아,
 * 앱에서는 로딩이 끝난 흰 화면으로 보인다. 실패가 실패처럼 보이지 않는 종류의 고장이다.
 *
 * <p><b>왜 항상 재인코딩하는가</b>: 세그먼트가 하나면 병합을 건너뛰고, 병합하더라도
 * {@code -c copy}라 재인코딩이 없다. 즉 기존 경로 어디에도 픽셀 포맷을 바로잡을 지점이 없었다.
 * 조건부로 판별해 필요할 때만 돌리려면 ffprobe 호출이 한 번 더 필요한데, 대상이 1초 미만
 * 수십 KB 영상이라 항상 재인코딩하는 편이 더 싸고 분기도 줄어든다.
 *
 * <p>실패해도 예외를 던지지 않고 원본 경로를 돌려준다. 이 단계는 호환성 보정이지 생성의
 * 일부가 아니므로, 여기서 막히면 이미 만들어진 영상까지 버리게 된다. 재생되지 않을지언정
 * 영상이 있는 편이 아무것도 없는 것보다 낫고, 실패는 로그로 남는다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class VideoPlaybackNormalizer {

    /**
     * 홀수 변을 하나 줄여 짝수로 만드는 scale filter다.
     *
     * 늘리지 않고 줄이는 이유는 도해 가장자리가 여백이라 1px 손실이 내용에 영향을 주지
     * 않는 반면, 늘리면 없는 픽셀을 만들어내야 하기 때문이다.
     */
    private static final String EVEN_DIMENSION_FILTER = "scale=trunc(iw/2)*2:trunc(ih/2)*2";

    private final FfmpegSettings ffmpegSettings;

    /**
     * 영상을 4:2:0 / 짝수 해상도로 다시 인코딩한 경로를 반환한다.
     *
     * 반환값이 입력과 다른 경로면 호출자가 임시 파일로 함께 정리해야 한다.
     * 보정에 실패하면 입력 경로를 그대로 반환한다.
     */
    public Path normalize(Path videoPath) {
        Path output = videoPath.resolveSibling("normalized_" + videoPath.getFileName());
        try {
            runFfmpeg(videoPath, output);
            if (!Files.exists(output) || Files.size(output) == 0) {
                log.warn("가이드 영상 호환 보정 결과가 비어 있어 원본을 사용한다: {}", videoPath);
                Files.deleteIfExists(output);
                return videoPath;
            }
            log.debug("가이드 영상 호환 보정 완료: {} -> {}", videoPath, output);
            return output;
        } catch (IOException | RuntimeException exception) {
            log.warn("가이드 영상 호환 보정 실패; 원본을 그대로 사용한다: {}", videoPath, exception);
            deleteQuietly(output);
            return videoPath;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("가이드 영상 호환 보정이 중단됐다; 원본을 그대로 사용한다: {}", videoPath);
            deleteQuietly(output);
            return videoPath;
        }
    }

    private void runFfmpeg(Path input, Path output) throws IOException, InterruptedException {
        String ffmpegPath = ffmpegSettings.getPath();
        if (ffmpegPath == null || ffmpegPath.isBlank()) {
            ffmpegPath = "ffmpeg";
        }

        ProcessBuilder builder = new ProcessBuilder(
                ffmpegPath,
                "-i", input.toString(),
                "-vf", EVEN_DIMENSION_FILTER,
                // 모바일이 하드웨어 디코딩할 수 있는 조합으로 고정한다.
                "-c:v", "libx264",
                "-profile:v", "high",
                "-pix_fmt", "yuv420p",
                // 가이드는 무음이다. 빈 audio track이 붙으면 일부 player가 재생을 지연시킨다.
                "-an",
                // moov atom을 앞으로 보내 전체 내려받기 전에 재생이 시작되게 한다.
                "-movflags", "+faststart",
                "-y",
                output.toString()
        );
        builder.redirectErrorStream(true);

        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("FFmpeg 호환 보정 실패 (exit code: " + exitCode + ")");
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 임시 파일 정리 실패는 보정 결과에 영향을 주지 않는다.
        }
    }
}
