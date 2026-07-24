package com.lingko.lingko.infra.pronunciation;

import com.lingko.lingko.core.config.FfmpegSettings;
import com.lingko.lingko.core.config.ReplicateSettings;
import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 생성된 발음 guide clip을 하나의 전달 가능한 media 파일로 병합한다.
 *
 * FFmpeg process와 임시 파일 관리를 도메인 서비스에서 분리하기 위해 전용 infrastructure component를 사용한다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class VideoMerger {
    private final FfmpegSettings ffmpegSettings;
    private final ReplicateSettings replicateSettings;

    /**
     * 여러 영상 파일을 하나로 병합
     *
     * @param segmentPaths 병합할 영상 파일 경로 리스트
     * @return 병합된 영상 파일 경로
     */
    public Path merge(List<Path> segmentPaths) {
        try {
            log.info("영상 병합 시작: {}개 세그먼트", segmentPaths.size());

            if (segmentPaths.isEmpty()) {
                throw new IllegalArgumentException("병합할 세그먼트가 없음");
            }

            // 단일 세그먼트면 그대로 반환
            if (segmentPaths.size() == 1) {
                log.info("단일 세그먼트 - 병합 생략");
                return segmentPaths.get(0);
            }

            // concat 파일 생성
            Path concatFile = createConcatFile(segmentPaths);

            // 출력 파일 생성
            Path outputFile = Files.createTempFile("merged_", ".mp4");

            try {
                // FFmpeg로 병합
                executeFFmpegConcat(concatFile, outputFile);

                log.info("영상 병합 완료: {}", outputFile);
                return outputFile;

            } finally {
                // concat 파일 삭제
                Files.deleteIfExists(concatFile);
            }

        } catch (Exception e) {
            log.error("영상 병합 실패", e);
            throw new VideoGenerationException("영상 병합 실패", e);
        }
    }

    /**
     * FFmpeg concat 파일 생성
     *
     * @param segmentPaths 세그먼트 파일 경로 리스트
     * @return concat 파일 경로
     */
    private Path createConcatFile(List<Path> segmentPaths) throws IOException {
        Path concatFile = Files.createTempFile("concat_", ".txt");

        try (BufferedWriter writer = Files.newBufferedWriter(concatFile)) {
            for (Path segmentPath : segmentPaths) {
                // FFmpeg concat 형식: file 'path/to/file.mp4'
                // Windows 경로 처리를 위해 / 로 변환
                String absolutePath = segmentPath.toAbsolutePath().toString().replace("\\", "/");
                writer.write(String.format("file '%s'%n", absolutePath));
            }
        }

        log.debug("Concat 파일 생성: {}", concatFile);
        return concatFile;
    }

    /**
     * FFmpeg concat 실행
     *
     * @param concatFile concat 파일 경로
     * @param outputFile 출력 파일 경로
     */
    private void executeFFmpegConcat(Path concatFile, Path outputFile)
            throws IOException, InterruptedException {

        int frameRate = replicateSettings.getFrameRate();

        String ffmpegPath = ffmpegSettings.getPath();
        if (ffmpegPath == null || ffmpegPath.isBlank()) {
            ffmpegPath = "ffmpeg";
        }

        // FFmpeg 명령어
        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-f", "concat",              // concat demuxer 사용
                "-safe", "0",                // 절대 경로 허용
                "-i", concatFile.toString(), // 입력 concat 파일
                "-c", "copy",                // 재인코딩 없이 복사 (빠름)
                "-r", String.valueOf(frameRate),  // Replicate 영상 프레임
                "-y",                        // 덮어쓰기
                outputFile.toString()        // 출력 파일
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        // FFmpeg 출력 로깅
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new VideoGenerationException(
                    "FFmpeg 실행 실패 (exit code: " + exitCode + ")"
            );
        }

        log.debug("FFmpeg concat 완료");
    }
}
