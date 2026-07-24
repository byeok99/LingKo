package com.lingko.lingko.core.util;

import com.lingko.lingko.core.config.AwsSettings;
import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Syllable 매핑 Util Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
public class SyllableMappingUtilTest {
    private SyllableMappingUtil util;

    @BeforeEach
    void setUp() {
        util = new SyllableMappingUtil();
        util.loadMapping();
    }

    @Test
    void testJsonLoad() {
        var mapping = util.getMapping("ㄱ");

        assertThat(mapping).isNotNull();
        assertThat(mapping.hasMouth() || mapping.hasTongue()).isTrue();
    }

    @Test
    void getImageUrlReturnsAbsoluteHttpsUrl() {
        String imageUrl = util.getImageUrl("ㅏ", VideoType.MOUTH);

        assertThat(imageUrl)
                .startsWith("https://lingko.s3.ap-northeast-2.amazonaws.com/guides/mouth/")
                .endsWith(".png");
    }

    @Test
    void getImageUrlUsesConfiguredS3BucketAndRegion() {
        SyllableMappingUtil configuredUtil = new SyllableMappingUtil(awsSettings("custom-bucket", "us-west-2"));
        configuredUtil.loadMapping();

        String imageUrl = configuredUtil.getImageUrl("ㅏ", VideoType.MOUTH);

        assertThat(imageUrl)
                .startsWith("https://custom-bucket.s3.us-west-2.amazonaws.com/guides/mouth/")
                .endsWith(".png");
    }

    private AwsSettings awsSettings(String bucket, String region) {
        AwsSettings settings = new AwsSettings();
        AwsSettings.S3 s3 = new AwsSettings.S3();
        s3.setBucket(bucket);
        s3.setRegion(region);
        settings.setS3(s3);
        return settings;
    }
}
