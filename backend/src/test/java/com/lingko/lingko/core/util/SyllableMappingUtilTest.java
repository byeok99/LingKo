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
        assertThat(util.getImageFrames("ㄱ", VideoType.TONGUE)).isNotEmpty();
    }

    @Test
    void imageFramesReturnAbsoluteHttpsUrls() {
        String imageUrl = util.getImageFrames("ㅏ", VideoType.MOUTH).getFirst();

        assertThat(imageUrl)
                .startsWith("https://lingko.s3.ap-northeast-2.amazonaws.com/guides/mouth/")
                .endsWith(".png");
    }

    @Test
    void imageFramesUseConfiguredS3BucketAndRegion() {
        SyllableMappingUtil configuredUtil = new SyllableMappingUtil(awsSettings("custom-bucket", "us-west-2"));
        configuredUtil.loadMapping();

        String imageUrl = configuredUtil.getImageFrames("ㅏ", VideoType.MOUTH).getFirst();

        assertThat(imageUrl)
                .startsWith("https://custom-bucket.s3.us-west-2.amazonaws.com/guides/mouth/")
                .endsWith(".png");
    }

    @Test
    void kimKeepsSourceTimelinesSynchronizedAndRemovesOnlyNoOpTransitions() {
        assertThat(util.createFrameSequence("김", VideoType.MOUTH))
                .hasSameSizeAs(util.createFrameSequence("김", VideoType.TONGUE));
        assertThat(util.createFramePairs("김", VideoType.MOUTH))
                .singleElement()
                .satisfies(pair -> assertThat(pair).hasSize(2));
        assertThat(util.createFramePairs("김", VideoType.TONGUE))
                .hasSize(2)
                .allSatisfy(pair -> assertThat(pair).hasSize(2));
    }

    @Test
    void diphthongsReuseExistingAssetsAsOrderedFrames() {
        assertThat(util.createFrameSequence("으", VideoType.MOUTH))
                .extracting(url -> url.substring(url.lastIndexOf('/') + 1))
                .containsExactly("vowel-eu.png");
        assertThat(util.createFrameSequence("의", VideoType.MOUTH))
                .extracting(url -> url.substring(url.lastIndexOf('/') + 1))
                .containsExactly("vowel-eu.png", "vowel-i.png");
        assertThat(util.createFrameSequence("와", VideoType.TONGUE))
                .extracting(url -> url.substring(url.lastIndexOf('/') + 1))
                .containsExactly("semi-vowel-w.png", "vowel-a.png");
    }

    @Test
    void hUsesTheFollowingVowelPostureWithoutPretendingToHaveAnOralShape() {
        assertThat(util.createFrameSequence("하", VideoType.TONGUE))
                .allMatch(url -> url.endsWith("vowel-a.png"))
                .hasSize(2);
        assertThat(util.getImageFrames("ㅎ", VideoType.TONGUE)).isEmpty();
    }

    @Test
    void repeatedPostureBecomesAStaticGuideInsteadOfAPointlessVideo() {
        assertThat(util.createFramePairs("하", VideoType.TONGUE))
                .singleElement()
                .satisfies(pair -> assertThat(pair)
                        .singleElement()
                        .asString()
                        .endsWith("vowel-a.png"));
    }

    @Test
    void commonSyllablesKeepExpectedMouthAndTongueTransitions() {
        assertThat(util.createFramePairs("한", VideoType.TONGUE))
                .singleElement()
                .satisfies(pair -> {
                    assertThat(pair.get(0)).contains("vowel-a");
                    assertThat(pair.get(1)).contains("alveolar-consonants");
                });
        assertThat(util.createFramePairs("밥", VideoType.MOUTH))
                .hasSize(2)
                .first()
                .satisfies(pair -> {
                    assertThat(pair.get(0)).contains("bilabial-consonants");
                    assertThat(pair.get(1)).contains("vowel-a");
                });
        assertThat(util.createFramePairs("국", VideoType.MOUTH))
                .singleElement()
                .satisfies(pair -> assertThat(pair)
                        .singleElement()
                        .asString()
                        .endsWith("vowel-u.png"));
        assertThat(util.createFramePairs("국", VideoType.TONGUE))
                .hasSize(2)
                .allSatisfy(pair -> assertThat(pair).hasSize(2));
    }

    @Test
    void sibilantOnsetUsesPalatalAllophoneOnlyBeforePalatalVowels() {
        String plainSibilant = util.createFramePairs("사", VideoType.TONGUE).getFirst().getFirst();
        String palatalSibilant = util.createFramePairs("시", VideoType.TONGUE).getFirst().getFirst();

        assertThat(plainSibilant).contains("alveolar-fricative");
        assertThat(palatalSibilant).contains("allophone-s-ss");
        assertThat(palatalSibilant).isNotEqualTo(plainSibilant);
    }

    @Test
    void emptyOrNullInputReturnsNoFramePairs() {
        assertThat(util.createFramePairs("", VideoType.MOUTH)).isEmpty();
        assertThat(util.createFramePairs(null, VideoType.TONGUE)).isEmpty();
    }

    @Test
    void onsetAndCodaRolesAreResolvedSeparately() {
        assertThat(util.resolveMappingKey("ㄹ", SyllableMappingUtil.SyllableRole.ONSET, null))
                .isEqualTo("초성ㄹ");
        assertThat(util.resolveMappingKey("ㄹ", SyllableMappingUtil.SyllableRole.CODA, null))
                .isEqualTo("종성ㄹ");
        assertThat(util.resolveMappingKey("ㅇ", SyllableMappingUtil.SyllableRole.CODA, null))
                .isEqualTo("종성ㅇ");
    }

    @Test
    void staticGuideUsesTheVowelTargetForBothTracks() {
        assertThat(util.getRepresentativeImageUrl("김", VideoType.MOUTH)).endsWith("vowel-i.png");
        assertThat(util.getRepresentativeImageUrl("김", VideoType.TONGUE)).endsWith("semi-vowel-y.png");
    }

    @Test
    void everyModernHangulSyllableHasSynchronizedMouthAndTongueTimelines() {
        for (char syllable = 0xAC00; syllable <= 0xD7A3; syllable++) {
            String text = String.valueOf(syllable);

            assertThat(util.createFrameSequence(text, VideoType.MOUTH))
                    .as("mouth frames for %s", text)
                    .isNotEmpty()
                    .hasSameSizeAs(util.createFrameSequence(text, VideoType.TONGUE));
        }
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
