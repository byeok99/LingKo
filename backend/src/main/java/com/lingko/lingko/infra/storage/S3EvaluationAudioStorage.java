package com.lingko.lingko.infra.storage;

import com.lingko.lingko.core.config.AwsSettings;
import com.lingko.lingko.core.config.EvaluationJobSettings;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationAudioStorage;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 사용자별 비공개 S3 object의 제한 시간 PUT 서명과 Worker 다운로드·삭제를 구현한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class S3EvaluationAudioStorage implements EvaluationAudioStorage {

    private static final String CONTENT_TYPE = "audio/wav";
    private static final long MIN_WAV_BYTES = 44;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AwsSettings awsSettings;
    private final EvaluationJobSettings settings;
    private final Clock clock;

    @Override
    public UploadTicket prepareUpload(
            Long userId,
            String fileName,
            String contentType,
            long contentLength
    ) {
        validateUploadMetadata(fileName, contentType, contentLength);
        String objectKey = userPrefix(userId) + UUID.randomUUID() + ".wav";
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket())
                .key(objectKey)
                .contentType(CONTENT_TYPE)
                .contentLength(contentLength)
                .build();
        Duration signatureDuration = Duration.ofMinutes(settings.getUploadUrlExpireMinutes());
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(signatureDuration)
                        .putObjectRequest(putRequest)
                        .build()
        );
        Instant expiresAt = clock.instant().plus(signatureDuration);
        return new UploadTicket(objectKey, presigned.url().toString(), expiresAt);
    }

    @Override
    public void validateUploaded(Long userId, String objectKey) {
        if (objectKey == null || !objectKey.startsWith(userPrefix(userId))) {
            throw new IllegalArgumentException("Uploaded audio does not belong to the authenticated user");
        }
        HeadObjectResponse object;
        try {
            object = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket())
                    .key(objectKey)
                    .build());
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new IllegalArgumentException("Uploaded audio is unavailable", exception);
            }
            throw new IllegalStateException("Failed to validate uploaded audio", exception);
        }
        if (object.contentLength() < MIN_WAV_BYTES
                || object.contentLength() > EvaluationService.MAX_AUDIO_BYTES
                || !CONTENT_TYPE.equalsIgnoreCase(object.contentType())) {
            throw new IllegalArgumentException("Uploaded audio metadata is invalid");
        }
    }

    @Override
    public Path download(String objectKey) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("lingko-evaluation-worker-", ".wav");
            // AWS SDK의 toFile은 기존 파일 덮어쓰기를 거부하므로 경로만 확보한 뒤 다운로드 전에 제거한다.
            Files.delete(tempFile);
            s3Client.getObject(
                    GetObjectRequest.builder().bucket(bucket()).key(objectKey).build(),
                    ResponseTransformer.toFile(tempFile)
            );
            return tempFile;
        } catch (RuntimeException | IOException exception) {
            deleteLocal(tempFile);
            throw new IllegalStateException("Failed to download evaluation audio", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket())
                    .key(objectKey)
                    .build());
        } catch (RuntimeException exception) {
            // Lifecycle가 최종 안전망이며 평가 결과를 S3 정리 실패로 되돌리지 않는다.
            log.warn("Failed to delete evaluation audio: objectKey={}", objectKey, exception);
        }
    }

    @Override
    public int deleteAllForUser(Long userId) {
        if (userId == null || userId < 1) {
            throw new IllegalArgumentException("userId must be positive");
        }

        try {
            String prefix = userPrefix(userId);
            return deleteCurrentObjects(prefix) + deleteObjectVersions(prefix);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Failed to delete account audio", exception);
        }
    }

    private int deleteCurrentObjects(String prefix) {
        int deletedCount = 0;
        while (true) {
            List<ObjectIdentifier> objects = s3Client.listObjectsV2(
                            ListObjectsV2Request.builder()
                                    .bucket(bucket())
                                    .prefix(prefix)
                                    .maxKeys(1_000)
                                    .build()
                    )
                    .contents()
                    .stream()
                    .map(object -> ObjectIdentifier.builder().key(object.key()).build())
                    .toList();
            if (objects.isEmpty()) {
                return deletedCount;
            }
            deleteObjects(objects);
            deletedCount += objects.size();
        }
    }

    private int deleteObjectVersions(String prefix) {
        int deletedCount = 0;
        while (true) {
            var response = s3Client.listObjectVersions(ListObjectVersionsRequest.builder()
                    .bucket(bucket())
                    .prefix(prefix)
                    .maxKeys(1_000)
                    .build());
            // Versioning 사용 여부와 관계없이 과거 원본과 delete marker까지 제거해야 탈퇴 즉시 삭제가 성립한다.
            List<ObjectIdentifier> objects = Stream.concat(
                            response.versions().stream()
                                    .map(version -> ObjectIdentifier.builder()
                                            .key(version.key())
                                            .versionId(version.versionId())
                                            .build()),
                            response.deleteMarkers().stream()
                                    .map(marker -> ObjectIdentifier.builder()
                                            .key(marker.key())
                                            .versionId(marker.versionId())
                                            .build())
                    )
                    .toList();
            if (objects.isEmpty()) {
                return deletedCount;
            }
            deleteObjects(objects);
            deletedCount += objects.size();
        }
    }

    private void deleteObjects(List<ObjectIdentifier> objects) {
        var response = s3Client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(bucket())
                .delete(Delete.builder()
                        .objects(objects)
                        .quiet(true)
                        .build())
                .build());
        if (!response.errors().isEmpty()) {
            throw new IllegalStateException("S3 rejected account audio deletion");
        }
    }

    @Override
    public void deleteLocal(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn("Failed to delete local evaluation audio: path={}", path, exception);
        }
    }

    private void validateUploadMetadata(String fileName, String contentType, long contentLength) {
        if (fileName == null
                || !fileName.toLowerCase(Locale.ROOT).endsWith(".wav")
                || !CONTENT_TYPE.equalsIgnoreCase(contentType)
                || contentLength < MIN_WAV_BYTES
                || contentLength > EvaluationService.MAX_AUDIO_BYTES) {
            throw new IllegalArgumentException("Only WAV audio up to 10 MiB can be uploaded");
        }
    }

    private String userPrefix(Long userId) {
        return "evaluation-audio/" + userId + "/";
    }

    private String bucket() {
        return awsSettings.getS3().getBucket();
    }
}
