package com.lingko.lingko.infra.storage;

import com.lingko.lingko.core.config.AwsSettings;
import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * S3 업로더
 *
 * 역할:
 * - 로컬 파일을 S3에 업로드
 * - URL에서 다운로드 후 S3에 업로드
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class S3Uploader {

    private final S3Client s3Client;
    private final AwsSettings awsSettings;
    private final ExternalMediaUrlValidator externalMediaUrlValidator;

    /**
     * 로컬 파일을 S3에 업로드
     *
     * @param filePath 로컬 파일 경로
     * @param s3Key S3 키
     * @return S3 URL
     */
    public String upload(String filePath, String s3Key) {
        try {
            String bucketName = awsSettings.getS3().getBucket();
            String region = awsSettings.getS3().getRegion();

            log.info("S3 업로드 시작: {} -> s3://{}/{}", filePath, bucketName, s3Key);

            Path path = Paths.get(filePath);

            if (!Files.exists(path)) {
                throw new VideoGenerationException("업로드할 파일이 존재하지 않음: " + filePath);
            }

            long fileSize = Files.size(path);
            log.debug("파일 크기: {} bytes ({} MB)", fileSize, fileSize / 1024 / 1024);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(getContentType(filePath))
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromFile(path)
            );

            String s3Url = publicUrl(s3Key);

            log.info("S3 업로드 완료: key={}", s3Key);
            return s3Url;

        } catch (S3Exception e) {
            log.error("S3 업로드 실패: {}", filePath, e);
            throw new VideoGenerationException("S3 업로드 실패: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("파일 읽기 실패: {}", filePath, e);
            throw new VideoGenerationException("파일 읽기 실패: " + filePath, e);
        }
    }

    /**
     * File 객체를 S3에 업로드
     *
     * @param file File 객체
     * @param s3Key S3 키
     * @return S3 URL
     */
    public String upload(File file, String s3Key) {
        return upload(file.getAbsolutePath(), s3Key);
    }

    /**
     * 결정적 key의 생성 가이드가 이미 존재하면 외부 생성 없이 재사용할 공개 URL을 반환한다.
     *
     * <p>조회에 실패해도 예외를 던지지 않고 "캐시 없음"으로 답한다. 이 호출은 생성을 건너뛸 수
     * 있는지 묻는 최적화이지 생성의 전제 조건이 아니다. 실패를 위로 올리면 호출자가 이를 생성
     * 실패로 처리해 정적 이미지로 강등하므로, 캐시 조회 한 번이 실제 가이드 생성을 막는다.
     *
     * <p>특히 IAM 정책에 {@code s3:ListBucket}이 없으면 S3는 없는 key에 404가 아니라 403을
     * 돌려준다. 404만 "없음"으로 보면 이 환경에서 캐시가 빈 순간부터 영상이 영영 만들어지지
     * 않으면서 오류도 드러나지 않는다. 잘못 판단했을 때의 대가는 이미 있는 파일을 다시 만드는
     * 비용뿐이라, 관대하게 처리하고 원인은 로그로 남긴다.
     */
    public Optional<String> findPublicUrl(String s3Key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(awsSettings.getS3().getBucket())
                    .key(s3Key)
                    .build());
            return Optional.of(publicUrl(s3Key));
        } catch (S3Exception exception) {
            // 404는 캐시가 없는 정상 상태다. 로그를 남기지 않는다.
            if (exception.statusCode() != 404) {
                log.warn(
                        "S3 guide cache lookup failed; regenerating: key={}, status={}",
                        s3Key,
                        exception.statusCode(),
                        exception
                );
            }
            return Optional.empty();
        } catch (SdkException exception) {
            // 자격증명·네트워크 문제도 같은 이유로 생성을 막지 않는다.
            log.warn("S3 guide cache lookup failed; regenerating: key={}", s3Key, exception);
            return Optional.empty();
        }
    }

    /**
     * URL에서 다운로드 후 S3에 업로드
     *
     * @param sourceUrl 다운로드할 URL
     * @param s3Key S3 키
     * @return S3 URL
     */
    public String uploadFromUrl(String sourceUrl, String s3Key) {
        Path tempFile = null;

        try {
            // presigned URL의 query에는 credential이 포함될 수 있어 URL 원문을 기록하지 않는다.
            log.info("외부 미디어 → S3 업로드 시작: key={}", s3Key);

            // 1. URL에서 임시 파일로 다운로드
            tempFile = downloadFromUrl(sourceUrl);

            // 2. 임시 파일을 S3에 업로드
            String s3Url = upload(tempFile.toString(), s3Key);

            log.info("외부 미디어 → S3 업로드 완료: key={}", s3Key);
            return s3Url;

        } finally {
            // 3. 임시 파일 삭제
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                    log.debug("임시 파일 삭제: {}", tempFile);
                } catch (IOException e) {
                    log.warn("임시 파일 삭제 실패: {}", tempFile, e);
                }
            }
        }
    }

    /**
     * URL에서 파일 다운로드
     *
     * @param url 다운로드할 URL
     * @return 다운로드된 임시 파일 경로
     */
    private Path downloadFromUrl(String url) {
        try {
            log.debug("외부 미디어 다운로드 시작");

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
            log.error("외부 미디어 다운로드 실패", e);
            throw new VideoGenerationException("외부 미디어 다운로드 실패", e);
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
     * 파일 확장자로 Content-Type 추론
     *
     * @param filePath 파일 경로
     * @return Content-Type
     */
    private String getContentType(String filePath) {
        String lowerPath = filePath.toLowerCase();

        if (lowerPath.endsWith(".mp4")) {
            return "video/mp4";
        } else if (lowerPath.endsWith(".avi")) {
            return "video/x-msvideo";
        } else if (lowerPath.endsWith(".mov")) {
            return "video/quicktime";
        } else if (lowerPath.endsWith(".webm")) {
            return "video/webm";
        } else if (lowerPath.endsWith(".png")) {
            return "image/png";
        } else if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerPath.endsWith(".gif")) {
            return "image/gif";
        }

        return "application/octet-stream";  // 기본값
    }

    private String publicUrl(String s3Key) {
        return String.format(
                "https://%s.s3.%s.amazonaws.com/%s",
                awsSettings.getS3().getBucket(),
                awsSettings.getS3().getRegion(),
                s3Key
        );
    }
}
