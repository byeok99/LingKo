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
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
}
