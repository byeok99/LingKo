package com.lingko.lingko.infra.pronunciation;

import com.lingko.lingko.core.config.AwsSettings;
import com.lingko.lingko.core.config.ReplicateSettings;
import com.lingko.lingko.core.config.S3Config;
import com.lingko.lingko.core.config.WebClientConfig;
import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.core.util.SyllableMappingUtil;
import com.lingko.lingko.infra.storage.S3Uploader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FrameInterpolationVideoGenerator 통합 테스트
 */
@SpringBootTest
class FrameInterpolationVideoGeneratorTest {

    @Autowired
    private FrameInterpolationVideoGenerator videoGenerator;

    @Autowired
    private SyllableMappingUtil syllableMappingUtil;

    @Test
    @DisplayName("SyllableMapping 로드 확인")
    void mapping_load_test() {
        // ㅂ
        SyllableMappingUtil.SyllableMapping bMapping = syllableMappingUtil.getMapping("ㅂ");

        System.out.println("=== ㅂ ===");
        System.out.println("Mouth: " + bMapping.getMouthUrl());
        System.out.println("Tongue: " + bMapping.getTongueUrl());

        assertThat(bMapping.hasMouth()).isTrue();
        assertThat(bMapping.hasTongue()).isTrue();

        // ㄱ
        SyllableMappingUtil.SyllableMapping gMapping = syllableMappingUtil.getMapping("ㄱ");

        System.out.println("\n=== ㄱ ===");
        System.out.println("Mouth: " + gMapping.getMouthUrl());
        System.out.println("Tongue: " + gMapping.getTongueUrl());

        assertThat(gMapping.hasMouth()).isFalse();  // 입 모양 없음
        assertThat(gMapping.hasTongue()).isTrue();
    }

    @Test
    @DisplayName("이미지 URL 생성")
    void image_url_test() {
        // ㅂ mouth
        String url = syllableMappingUtil.getImageUrl("ㅂ", VideoType.MOUTH);

        System.out.println("ㅂ mouth URL: " + url);
        assertThat(url).contains("guides/mouth/");
        assertThat(url).contains("bilabial-consonants.png");

        // ㄱ mouth (없음)
        String gMouth = syllableMappingUtil.getImageUrl("ㄱ", VideoType.MOUTH);

        System.out.println("ㄱ mouth URL: " + gMouth);
        assertThat(gMouth).isNull();
    }

    @Test
    @DisplayName("프레임 쌍 생성 - 가 (ㄱ+ㅏ)")
    void frame_pairs_ga_test() {
        // 가 = ㄱ + ㅏ (tongue)
        List<String> phonemes = List.of("ㄱ", "ㅏ");
        List<List<String>> framePairs = syllableMappingUtil.createFramePairs(phonemes, VideoType.TONGUE);

        System.out.println("=== 가 (tongue) ===");
        System.out.println("프레임 쌍 수: " + framePairs.size());
        framePairs.forEach(pair -> {
            System.out.println("  " + pair.get(0).substring(pair.get(0).lastIndexOf('/') + 1));
            System.out.println("  → " + pair.get(1).substring(pair.get(1).lastIndexOf('/') + 1));
        });

        assertThat(framePairs).hasSize(1);
    }

    @Test
    @DisplayName("프레임 쌍 생성 - 가 (mouth, ㄱ 없음)")
    void frame_pairs_ga_mouth_test() {
        // 가 = ㄱ + ㅏ (mouth)
        // ㄱ mouth 없음 → ㅏ만
        List<String> phonemes = List.of("ㄱ", "ㅏ");
        List<List<String>> framePairs = syllableMappingUtil.createFramePairs(phonemes, VideoType.MOUTH);

        System.out.println("=== 가 (mouth) ===");
        System.out.println("프레임 쌍: " + framePairs);

        // [[ㅏ]] (정적 이미지)
        assertThat(framePairs).hasSize(1);
        assertThat(framePairs.get(0)).hasSize(1);
    }

    @Test
    @DisplayName("영상 생성 - 바 (ㅂ+ㅏ)")
    void video_ba_test() {
        // 바 = ㅂ + ㅏ
        List<String> phonemes = List.of("ㅂ", "ㅏ");
        List<List<String>> framePairs = syllableMappingUtil.createFramePairs(phonemes, VideoType.MOUTH);

        System.out.println("\n=== 바 (mouth) ===");
        System.out.println("프레임 쌍 수: " + framePairs.size());

        String s3Url = videoGenerator.generate(framePairs, "바", VideoType.MOUTH);

        System.out.println("S3 URL: " + s3Url);
        assertThat(s3Url).contains("videos/mouth");
        assertThat(s3Url).contains("바");
    }

    @Test
    @DisplayName("영상 생성 - 각 (ㄱ+ㅏ+ㄱ, 병합)")
    void video_gak_test() {
        // 각 = ㄱ + ㅏ + ㄱ
        List<String> phonemes = List.of("ㄱ", "ㅏ", "ㄱ");
        List<List<String>> framePairs = syllableMappingUtil.createFramePairs(phonemes, VideoType.TONGUE);

        System.out.println("\n=== 각 (tongue) ===");
        System.out.println("프레임 쌍 수: " + framePairs.size());

        String s3Url = videoGenerator.generate(framePairs, "각", VideoType.TONGUE);

        System.out.println("S3 URL: " + s3Url);
        assertThat(s3Url).contains("videos/tongue");
        assertThat(framePairs).hasSize(2);  // 병합됨
    }

    @Test
    @DisplayName("정적 이미지 - ㅂ")
    void static_image_b_test() {
        // ㅂ
        List<String> phonemes = List.of("ㅂ");
        List<List<String>> framePairs = syllableMappingUtil.createFramePairs(phonemes, VideoType.MOUTH);

        System.out.println("\n=== ㅂ (mouth) ===");
        System.out.println("프레임 쌍: " + framePairs);

        String s3Url = videoGenerator.generate(framePairs, "ㅂ", VideoType.MOUTH);

        System.out.println("S3 URL: " + s3Url);
        assertThat(s3Url).contains("images/mouth");  // 정적 이미지
    }

    @Test
    @DisplayName("전체 플로우 - 밥 (ㅂ+ㅏ+ㅂ)")
    void full_flow_bab_test() {
        // 밥 = ㅂ + ㅏ + ㅂ
        List<String> phonemes = List.of("ㅂ", "ㅏ", "ㅂ");

        System.out.println("\n=== 밥 (mouth) 전체 플로우 ===");

        // 1. 프레임 쌍 생성
        List<List<String>> framePairs = syllableMappingUtil.createFramePairs(phonemes, VideoType.MOUTH);

        System.out.println("프레임 쌍 수: " + framePairs.size());
        framePairs.forEach(pair -> {
            String file1 = pair.get(0).substring(pair.get(0).lastIndexOf('/') + 1);
            String file2 = pair.get(1).substring(pair.get(1).lastIndexOf('/') + 1);
            System.out.println("  " + file1 + " → " + file2);
        });

        // 2. 영상 생성
        String s3Url = videoGenerator.generate(framePairs, "밥", VideoType.MOUTH);

        // 3. 검증
        System.out.println("\n생성된 S3 URL: " + s3Url);
        assertThat(s3Url).isNotNull();
        assertThat(s3Url).contains("videos/mouth");
        assertThat(s3Url).contains("밥");
        assertThat(s3Url).endsWith(".mp4");

        System.out.println("\n전체 플로우 성공!");
    }

    @Test
    @DisplayName("복잡한 음절 - 강 (ㄱ+ㅏ+ㅇ)")
    void complex_syllable_gang_test() {
        // 강 = ㄱ + ㅏ + ㅇ
        List<String> phonemes = List.of("ㄱ", "ㅏ", "ㅇ");

        System.out.println("\n=== 강 (tongue) ===");

        List<List<String>> framePairs = syllableMappingUtil.createFramePairs(phonemes, VideoType.TONGUE);

        System.out.println("자모: " + phonemes);
        System.out.println("프레임 쌍 수: " + framePairs.size());
        framePairs.forEach(pair -> {
            String file1 = pair.get(0).substring(pair.get(0).lastIndexOf('/') + 1);
            String file2 = pair.get(1).substring(pair.get(1).lastIndexOf('/') + 1);
            System.out.println("  " + file1 + " → " + file2);
        });

        String s3Url = videoGenerator.generate(framePairs, "강", VideoType.TONGUE);

        System.out.println("S3 URL: " + s3Url);
        assertThat(s3Url).isNotNull();
        assertThat(framePairs).hasSize(2);  // ㄱ→ㅏ, ㅏ→ㅇ
    }
}