package com.lingko.lingko.core.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.lingko.lingko.core.domain.evaluation.dto.VideoType;

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
}
