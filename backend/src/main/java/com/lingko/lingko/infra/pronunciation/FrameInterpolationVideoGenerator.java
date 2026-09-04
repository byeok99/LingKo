package com.lingko.lingko.infra.pronunciation;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import com.lingko.lingko.core.domain.evaluation.service.VideoGenerator;
import com.lingko.lingko.infra.storage.ExternalMediaUrlValidator;
import com.lingko.lingko.infra.storage.S3Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Frame Interpolation 영상 생성기
 *
 * 역할:
 * 1. 정적 이미지 처리 (단일 프레임)
 * 2. 영상 생성 (Frame Interpolation)
 * 3. 세그먼트 병합
 * 4. 모바일 재생 호환 보정
 * 5. S3 업로드
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FrameInterpolationVideoGenerator implements VideoGenerator {

    private static final int GENERATION_LOCK_STRIPES = 64;

    private final ReplicateApiClient replicateApiClient;
    private final VideoMerger videoMerger;
    private final S3Uploader s3Uploader;
    private final ExternalMediaUrlValidator externalMediaUrlValidator;
    private final VideoPlaybackNormalizer videoPlaybackNormalizer;
    private final List<Object> generationLocks = IntStream
            .range(0, GENERATION_LOCK_STRIPES)
            .mapToObj(ignored -> new Object())
            .toList();

    @Override
    public String generate(List<List<String>> urlPairs, String syllable, VideoType type) {
        try {
            // 빈 리스트 체크
            if (urlPairs == null || urlPairs.isEmpty()) {
                throw new IllegalArgumentException("URL 쌍이 비어있음: " + syllable);
            }

            log.info("영상 생성 시작: {} {}, {}개 세그먼트", syllable, type, urlPairs.size());

            // 정적 이미지 케이스 (단일 프레임)
            if (urlPairs.size() == 1 && urlPairs.get(0).size() == 1) {
                return handleStaticImage(urlPairs.get(0).get(0), syllable, type);
            }

            // Frame Interpolation 케이스
            return handleVideoGeneration(urlPairs, syllable, type);

        } catch (VideoGenerationException e) {
            // VideoGenerationException은 그대로 던짐
            throw e;
        } catch (Exception e) {
            log.error("영상 생성 실패: {} {}", syllable, type, e);
            throw new VideoGenerationException("영상 생성 실패: " + syllable, e);
        }
    }

    /**
     * 정적 이미지 처리
     *
     * 영상 변환 없이 이미지를 S3에 업로드
     */
    private String handleStaticImage(String imageUrl, String syllable, VideoType type) {
        // 외부 URL은 query credential을 포함할 수 있어 syllable과 type만 감사 정보로 남긴다.
        log.info("정적 이미지 처리: syllable={}, type={}", syllable, type);

        externalMediaUrlValidator.validate(imageUrl);

        // 파일명 생성
        String extension = extractExtension(imageUrl);
        String fileName = generateImageFileName(syllable, type, extension);
        String s3Key = "images/" + type.getPrefix() + "/" + fileName;

        // URL → S3 업로드
        String s3Url = s3Uploader.uploadFromUrl(imageUrl, s3Key);

        log.info("정적 이미지 완료: syllable={}, type={}", syllable, type);
        return s3Url;
    }

    /**
     * 영상 생성 처리
     *
     * Frame Interpolation 후 S3 업로드
     */
    private String handleVideoGeneration(List<List<String>> urlPairs, String syllable, VideoType type) {
        log.info("영상 생성 처리: {}개 세그먼트", urlPairs.size());

        String fileName = generateVideoFileName(syllable, type, urlPairs);
        String s3Key = "videos/" + type.getPrefix() + "/" + fileName;
        Object generationLock = generationLocks.get(
                Math.floorMod(s3Key.hashCode(), GENERATION_LOCK_STRIPES)
        );
        // 제거되는 key별 lock의 대기자 경쟁을 피하고 메모리를 제한하기 위해 고정 stripe를 사용한다.
        synchronized (generationLock) {
            return generateOrReuseVideo(urlPairs, syllable, type, s3Key);
        }
    }

    private String generateOrReuseVideo(
            List<List<String>> urlPairs,
            String syllable,
            VideoType type,
            String s3Key
    ) {
        String cachedUrl = s3Uploader.findPublicUrl(s3Key).orElse(null);
        if (cachedUrl != null) {
            log.info("기존 가이드 영상 재사용: {} {}", syllable, type);
            return cachedUrl;
        }

        List<Path> tempFiles = new ArrayList<>();

        try {
            // 1. 각 프레임 쌍으로 영상 생성 및 다운로드
            List<Path> segmentPaths = generateAndDownloadSegments(urlPairs);
            tempFiles.addAll(segmentPaths);

            // 2. 여러 세그먼트면 병합, 1개면 그대로
            Path finalVideoPath;
            if (segmentPaths.size() == 1) {
                finalVideoPath = segmentPaths.get(0);
                log.info("세그먼트 1개 - 병합 생략");
            } else {
                finalVideoPath = videoMerger.merge(segmentPaths);
                tempFiles.add(finalVideoPath);
                log.info("세그먼트 {}개 병합 완료", segmentPaths.size());
            }

            // 3. 모바일이 디코딩할 수 있는 형식으로 보정한다.
            //    병합을 건너뛰거나(-세그먼트 1개) 병합하더라도 -c copy라 재인코딩이 없어,
            //    여기가 픽셀 포맷을 바로잡을 수 있는 유일한 지점이다.
            Path uploadPath = videoPlaybackNormalizer.normalize(finalVideoPath);
            if (!uploadPath.equals(finalVideoPath)) {
                tempFiles.add(uploadPath);
            }

            // 4. 최종 영상을 S3에 업로드
            String s3Url = s3Uploader.upload(uploadPath.toString(), s3Key);

            log.info("영상 생성 완료: syllable={}, type={}", syllable, type);
            return s3Url;

        } finally {
            // 5. 모든 임시 파일 삭제
            cleanupTempFiles(tempFiles);
        }
    }

    /**
     * 여러 프레임 쌍으로 영상 생성 및 다운로드
     */
    private List<Path> generateAndDownloadSegments(List<List<String>> urlPairs) {
        List<Path> segmentPaths = new ArrayList<>();

        for (int i = 0; i < urlPairs.size(); i++) {
            List<String> pair = urlPairs.get(i);

            if (pair.size() != 2) {
                // URL 원문을 예외·로그에 섞지 않고 구조 오류만 알린다.
                throw new IllegalArgumentException("영상 생성은 세그먼트당 2개 프레임이 필요함");
            }

            log.info("세그먼트 {}/{} 생성", i + 1, urlPairs.size());

            externalMediaUrlValidator.validate(pair.get(0));
            externalMediaUrlValidator.validate(pair.get(1));

            // Replicate API로 Frame Interpolation
            String videoUrl = replicateApiClient.interpolate(pair.get(0), pair.get(1));

            // 영상 다운로드
            Path segmentPath = downloadFromUrl(videoUrl);
            segmentPaths.add(segmentPath);

            log.info("세그먼트 {}/{} 완료", i + 1, urlPairs.size());
        }

        return segmentPaths;
    }

    /**
     * URL에서 파일 다운로드
     *
     * @param url 다운로드할 URL
     * @return 다운로드된 임시 파일 경로
     */
    private Path downloadFromUrl(String url) {
        try {
            // 공급자 출력 URL도 query credential을 포함할 수 있어 로그에 기록하지 않는다.
            log.debug("생성 영상 다운로드 시작");

            // 확장자 추출
            String extension = extractExtension(url);

            // 임시 파일 생성
            Path tempFile = Files.createTempFile("download_", extension);

            HttpURLConnection connection = externalMediaUrlValidator.openConnection(url);
            try (InputStream in = connection.getInputStream();
                 OutputStream out = Files.newOutputStream(tempFile)) {
                copyWithLimit(in, out, ExternalMediaUrlValidator.MAX_DOWNLOAD_BYTES);
            } finally {
                connection.disconnect();
            }

            long fileSize = Files.size(tempFile);
            log.debug("다운로드 완료: {} ({} bytes)", tempFile, fileSize);

            return tempFile;

        } catch (IOException e) {
            log.error("생성 영상 다운로드 실패", e);
            throw new VideoGenerationException("생성 영상 다운로드 실패", e);
        }
    }

    private void copyWithLimit(InputStream in, OutputStream out, long maxBytes) throws IOException {
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;

        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new VideoGenerationException("외부 미디어 크기 제한 초과: " + total);
            }
            out.write(buffer, 0, read);
        }
    }

    /**
     * 임시 파일들 정리
     */
    private void cleanupTempFiles(List<Path> tempFiles) {
        for (Path tempFile : tempFiles) {
            try {
                Files.deleteIfExists(tempFile);
                log.debug("임시 파일 삭제: {}", tempFile);
            } catch (IOException e) {
                log.warn("임시 파일 삭제 실패: {}", tempFile, e);
            }
        }
    }

    /**
     * URL에서 확장자 추출
     *
     * @param url URL
     * @return 확장자 (예: .mp4, .png)
     */
    private String extractExtension(String url) {
        // 쿼리 파라미터 제거
        String cleanUrl = url.split("\\?")[0];

        // 확장자 추출
        int lastDot = cleanUrl.lastIndexOf('.');
        if (lastDot > 0) {
            return cleanUrl.substring(lastDot);  // .mp4, .png 등
        }

        return ".mp4";  // 기본값
    }

    /**
     * 영상 파일명 생성
     *
     * 동일 음절·타입·프레임은 재시작 후에도 같은 S3 object를 조회하도록 내용 기반 파일명을 사용한다.
     */
    private String generateVideoFileName(
            String syllable,
            VideoType type,
            List<List<String>> urlPairs
    ) {
        StringBuilder source = new StringBuilder()
                .append(type.name())
                .append('|')
                .append(syllable);
        urlPairs.forEach(pair -> source.append('|').append(String.join(",", pair)));

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.toString().getBytes(StandardCharsets.UTF_8));
            return String.format(
                    "%s_%s.mp4",
                    type.getPrefix(),
                    HexFormat.of().formatHex(digest, 0, 12)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /**
     * 이미지 파일명 생성
     *
     * 형식: {type}_{syllable}_{uuid}{extension}
     */
    private String generateImageFileName(String syllable, VideoType type, String extension) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%s_%s%s", type.getPrefix(), syllable, uuid, extension);
    }

}
