package com.lingko.lingko.core.util;

import com.lingko.lingko.core.config.AwsSettings;
import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

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
