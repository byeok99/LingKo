package com.lingko.lingko.infra.storage;

import com.lingko.lingko.core.config.AwsSettings;
import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 생성 가이드가 S3에 이미 있는지 확인하고 공개 URL로 복원하는 계약을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class S3UploaderTest {

    @Mock
    private S3Client s3Client;
    @Mock
    private ExternalMediaUrlValidator externalMediaUrlValidator;

    private S3Uploader uploader;

    @BeforeEach
    void setUp() {
        AwsSettings settings = new AwsSettings();
        AwsSettings.S3 s3 = new AwsSettings.S3();
        s3.setBucket("guide-bucket");
        s3.setRegion("ap-northeast-2");
        settings.setS3(s3);
        uploader = new S3Uploader(s3Client, settings, externalMediaUrlValidator);
    }

    @Test
    @DisplayName("기존 object는 앱이 사용할 공개 URL로 반환한다")
    void returnsPublicUrlForExistingObject() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        Optional<String> result = uploader.findPublicUrl("videos/mouth/kim.mp4");

        assertThat(result).contains(
                "https://guide-bucket.s3.ap-northeast-2.amazonaws.com/videos/mouth/kim.mp4"
        );
    }

    @Test
    @DisplayName("object가 없으면 생성할 수 있도록 빈 결과를 반환한다")
    void returnsEmptyForMissingObject() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).message("Not Found").build());

        assertThat(uploader.findPublicUrl("videos/mouth/kim.mp4")).isEmpty();
    }

    @Test
    @DisplayName("S3 장애는 캐시 미스로 숨기지 않는다")
    void propagatesStorageFailure() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(503).message("Unavailable").build());

        assertThatThrownBy(() -> uploader.findPublicUrl("videos/mouth/kim.mp4"))
                .isInstanceOf(VideoGenerationException.class);
    }
}
