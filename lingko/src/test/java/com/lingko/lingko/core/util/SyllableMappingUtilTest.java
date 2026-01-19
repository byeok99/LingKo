package com.lingko.lingko.core.util;

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
        assertThat(mapping.hasLips() || mapping.hasTongue()).isTrue();
    }
}
