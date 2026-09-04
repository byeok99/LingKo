package com.lingko.lingko.infra.storage;

import com.lingko.lingko.core.config.AwsSettings;
import com.lingko.lingko.core.config.EvaluationJobSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.DeleteMarkerEntry;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S3 업로드 검증 실패가 내부 오류가 아니라 클라이언트 입력 오류로 정규화되는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class S3EvaluationAudioStorageTest {

    @Mock
    private S3Client s3Client;
    @Mock
    private S3Presigner s3Presigner;

    private S3EvaluationAudioStorage storage;

    @BeforeEach
    void setUp() {
        AwsSettings awsSettings = new AwsSettings();
        AwsSettings.S3 s3 = new AwsSettings.S3();
        s3.setBucket("test-bucket");
        awsSettings.setS3(s3);
        storage = new S3EvaluationAudioStorage(
                s3Client,
                s3Presigner,
                awsSettings,
                new EvaluationJobSettings(),
                Clock.systemUTC()
        );
    }

    @Test
    @DisplayName("업로드되지 않은 S3 object로 작업을 만들면 400 대상 입력 오류로 변환한다")
    void rejectsMissingUploadedObject() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).message("Not Found").build());

        assertThatThrownBy(() -> storage.validateUploaded(
                7L,
                "evaluation-audio/7/missing.wav"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Uploaded audio is unavailable");
    }

    @Test
    @DisplayName("S3 서비스 장애는 사용자 입력 오류로 숨기지 않는다")
    void preservesStorageServiceFailure() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(503).message("Unavailable").build());

        assertThatThrownBy(() -> storage.validateUploaded(
                7L,
                "evaluation-audio/7/audio.wav"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to validate uploaded audio");
    }

    @Test
    @DisplayName("회원 탈퇴 시 사용자 prefix의 모든 S3 object를 batch 삭제한다")
    void deletesAllObjectsForUserPrefix() {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(
                        ListObjectsV2Response.builder()
                                .contents(
                                        S3Object.builder().key("evaluation-audio/7/first.wav").build(),
                                        S3Object.builder().key("evaluation-audio/7/second.wav").build()
                                )
                                .build(),
                        ListObjectsV2Response.builder().build()
                );
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenReturn(DeleteObjectsResponse.builder().build());
        when(s3Client.listObjectVersions(any(ListObjectVersionsRequest.class)))
                .thenReturn(ListObjectVersionsResponse.builder().build());

        int deletedCount = storage.deleteAllForUser(7L);

        var listRequest = forClass(ListObjectsV2Request.class);
        verify(s3Client, org.mockito.Mockito.times(2)).listObjectsV2(listRequest.capture());
        assertThat(listRequest.getAllValues())
                .allSatisfy(request -> assertThat(request.prefix()).isEqualTo("evaluation-audio/7/"));

        var deleteRequest = forClass(DeleteObjectsRequest.class);
        verify(s3Client).deleteObjects(deleteRequest.capture());
        assertThat(deleteRequest.getValue().delete().objects())
                .extracting(object -> object.key())
                .containsExactlyInAnyOrder(
                        "evaluation-audio/7/first.wav",
                        "evaluation-audio/7/second.wav"
                );
        assertThat(deletedCount).isEqualTo(2);
    }

    @Test
    @DisplayName("회원 탈퇴 시 Versioning 버킷의 과거 object와 delete marker도 삭제한다")
    void deletesAllObjectVersionsForUserPrefix() {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().build());
        when(s3Client.listObjectVersions(any(ListObjectVersionsRequest.class)))
                .thenReturn(
                        ListObjectVersionsResponse.builder()
                                .versions(ObjectVersion.builder()
                                        .key("evaluation-audio/7/audio.wav")
                                        .versionId("version-1")
                                        .build())
                                .deleteMarkers(DeleteMarkerEntry.builder()
                                        .key("evaluation-audio/7/audio.wav")
                                        .versionId("marker-1")
                                        .build())
                                .build(),
                        ListObjectVersionsResponse.builder().build()
                );
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenReturn(DeleteObjectsResponse.builder().build());

        int deletedCount = storage.deleteAllForUser(7L);

        var listRequest = forClass(ListObjectVersionsRequest.class);
        verify(s3Client, org.mockito.Mockito.times(2)).listObjectVersions(listRequest.capture());
        assertThat(listRequest.getAllValues())
                .allSatisfy(request -> assertThat(request.prefix()).isEqualTo("evaluation-audio/7/"));

        var deleteRequest = forClass(DeleteObjectsRequest.class);
        verify(s3Client).deleteObjects(deleteRequest.capture());
        assertThat(deleteRequest.getValue().delete().objects())
                .extracting(object -> object.key() + ":" + object.versionId())
                .containsExactlyInAnyOrder(
                        "evaluation-audio/7/audio.wav:version-1",
                        "evaluation-audio/7/audio.wav:marker-1"
                );
        assertThat(deletedCount).isEqualTo(2);
    }

    @Test
    @DisplayName("회원 탈퇴 S3 정리가 실패하면 DB 삭제를 막을 수 있도록 예외를 전파한다")
    void propagatesAccountAudioCleanupFailure() {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenThrow(S3Exception.builder().statusCode(503).message("Unavailable").build());

        assertThatThrownBy(() -> storage.deleteAllForUser(7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to delete account audio");
    }
}
