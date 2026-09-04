package com.lingko.lingko.infra.storage;

import com.lingko.lingko.core.config.AwsSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    @DisplayName("ListBucket 권한이 없어 없는 key에 403이 와도 생성을 막지 않는다")
    void treatsForbiddenAsCacheMiss() {
        // IAM 정책에 s3:ListBucket이 없으면 S3는 없는 key에 404가 아니라 403을 준다.
        // 404만 캐시 미스로 보면 이 환경에서는 캐시가 빈 순간부터 영상이 영영 생성되지 않는다.
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(403).message("Forbidden").build());

        assertThat(uploader.findPublicUrl("videos/mouth/kim.mp4")).isEmpty();
    }

    @Test
    @DisplayName("S3 장애와 SDK 오류도 캐시 미스로 답해 생성 경로를 열어둔다")
    void treatsStorageFailureAsCacheMiss() {
        // 이 호출은 생성을 건너뛸 수 있는지 묻는 최적화이지 생성의 전제 조건이 아니다.
        // 실패를 위로 올리면 호출자가 생성 실패로 처리해 정적 이미지로 강등하므로,
        // 캐시 조회 한 번이 실제 가이드 생성을 막는다. 잘못 판단해도 대가는 재생성 비용뿐이다.
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(503).message("Unavailable").build());
        assertThat(uploader.findPublicUrl("videos/mouth/kim.mp4")).isEmpty();

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(SdkClientException.builder().message("no credentials").build());
        assertThat(uploader.findPublicUrl("videos/mouth/kim.mp4")).isEmpty();
    }
}
