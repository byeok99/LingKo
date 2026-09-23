package com.lingko.lingko.core.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.core.config.AwsSettings;
import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 기존 입·혀 이미지를 역할별 프레임 시퀀스로 해석하는 매핑 경계다.
 *
 * 초성·중성·종성을 구분하고 복합 모음을 여러 기존 이미지로 조합한다. 특정 modality에 이미지가 없는
 * 자음은 인접 자세를 유지해 입·혀 영상의 시간축을 같게 하되, 존재하지 않는 조음 위치를 만들어내지 않는다.
 */
@Slf4j
@Component
public class SyllableMappingUtil {

    private static final String S3_BASE_URL = "https://lingko.s3.ap-northeast-2.amazonaws.com/guides";
    private static final Set<String> PALATAL_VOWELS = Set.of("ㅣ", "ㅑ", "ㅒ", "ㅕ", "ㅖ", "ㅛ", "ㅠ");
    private static final SyllableMapping EMPTY_MAPPING = new SyllableMapping(List.of(), List.of());

    private final AwsSettings awsSettings;
    private Map<String, SyllableMapping> mappingTable = Map.of();

    public SyllableMappingUtil(AwsSettings awsSettings) {
        this.awsSettings = awsSettings;
    }

    SyllableMappingUtil() {
        this(null);
    }

    /** 초성·중성·종성은 같은 자모라도 실제 역할이 달라질 수 있다. */
    public enum SyllableRole {
        ONSET,
        NUCLEUS,
        CODA
    }

    @PostConstruct
    public void loadMapping() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ClassPathResource resource = new ClassPathResource("syllable_mapping.json");
            Map<String, Map<String, List<String>>> rawData = mapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<>() { }
            );
            mappingTable = rawData.entrySet().stream()
                    .collect(java.util.stream.Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            entry -> toMapping(entry.getKey(), entry.getValue())
                    ));
            log.info(
                    "syllable_mapping.json 로드 완료: {}개, version={}",
                    mappingTable.size(),
                    GuideMediaVersion.CURRENT
            );
        } catch (IOException exception) {
            log.error("syllable_mapping.json 로드 실패", exception);
            throw new IllegalStateException("Failed to load syllable mapping", exception);
        }
    }

    private SyllableMapping toMapping(String key, Map<String, List<String>> rawMapping) {
        List<String> mouthFrames = immutableFrames(rawMapping.get("mouth_frames"));
        List<String> tongueFrames = immutableFrames(rawMapping.get("tongue_frames"));
        int mouthTimelineSize = Math.max(1, mouthFrames.size());
        int tongueTimelineSize = Math.max(1, tongueFrames.size());
        if (mouthTimelineSize != tongueTimelineSize) {
            throw new IllegalStateException("입·혀 프레임 수가 다른 매핑: " + key);
        }
        return new SyllableMapping(mouthFrames, tongueFrames);
    }

    private List<String> immutableFrames(List<String> frames) {
        if (frames == null) {
            return List.of();
        }
        return frames.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    private SyllableMapping getMapping(String key) {
        return mappingTable.getOrDefault(key, EMPTY_MAPPING);
    }

    /**
     * 매핑 key 자체의 프레임을 절대 URL로 반환한다. 빈 목록은 해당 부위로 표현할 자세가 없다는 뜻이다.
     */
    public List<String> getImageFrames(String key, VideoType type) {
        SyllableMapping mapping = getMapping(key);
        String folder = type == VideoType.MOUTH ? "mouth" : "tongue";
        return mapping.frames(type).stream()
                .map(filename -> String.format("%s/%s/%s", getGuideBaseUrl(), folder, filename))
                .toList();
    }

    /** 자모가 놓인 음절 역할과 다음 모음을 이용해 문맥 매핑 key를 결정한다. */
    public String resolveMappingKey(String phoneme, SyllableRole role, String nextVowel) {
        if (role == SyllableRole.ONSET && "ㄹ".equals(phoneme)) {
            return "초성ㄹ";
        }
        if (role == SyllableRole.CODA && "ㄹ".equals(phoneme)) {
            return "종성ㄹ";
        }
        if (role == SyllableRole.CODA && "ㅇ".equals(phoneme)) {
            return "종성ㅇ";
        }
        if (role == SyllableRole.ONSET
                && ("ㅅ".equals(phoneme) || "ㅆ".equals(phoneme))
                && PALATAL_VOWELS.contains(nextVowel)) {
            return "변이음" + phoneme;
        }
        return phoneme;
    }

    /**
     * 한 음절을 시간순 프레임으로 펼친다. 이미지가 없는 자음 구간은 가장 가까운 실제 자세를 유지한다.
     */
    public List<String> createFrameSequence(String syllable, VideoType type) {
        KoreanPhonemeUtil.HangulChar decomposed = decomposeSingleSyllable(syllable);
        if (decomposed == null) {
            return List.of();
        }

        List<String> frames = new ArrayList<>();
        if (!"ㅇ".equals(decomposed.getChosung())) {
            appendRoleFrames(
                    frames,
                    resolveMappingKey(decomposed.getChosung(), SyllableRole.ONSET, decomposed.getJungsung()),
                    type
            );
        }
        appendRoleFrames(
                frames,
                resolveMappingKey(decomposed.getJungsung(), SyllableRole.NUCLEUS, null),
                type
        );
        if (!decomposed.getJongsung().isEmpty()) {
            appendRoleFrames(
                    frames,
                    resolveMappingKey(decomposed.getJongsung(), SyllableRole.CODA, null),
                    type
            );
        }
        return fillMissingFrames(frames);
    }

    /** 준비 화면은 입·혀가 서로 다른 시점을 가리키지 않도록 중성의 마지막 목표 자세를 사용한다. */
    public String getRepresentativeImageUrl(String syllable, VideoType type) {
        KoreanPhonemeUtil.HangulChar decomposed = decomposeSingleSyllable(syllable);
        if (decomposed == null) {
            return null;
        }
        List<String> nucleusFrames = getImageFrames(
                resolveMappingKey(decomposed.getJungsung(), SyllableRole.NUCLEUS, null),
                type
        );
        return nucleusFrames.isEmpty() ? null : nucleusFrames.get(nucleusFrames.size() - 1);
    }

    /** Frame Interpolation 공급자가 요구하는 인접 프레임 쌍으로 변환한다. */
    public List<List<String>> createFramePairs(String syllable, VideoType type) {
        List<String> imageUrls = collapseRepeatedPostures(createFrameSequence(syllable, type));
        if (imageUrls.isEmpty()) {
            return List.of();
        }
        if (imageUrls.size() == 1) {
            return List.of(List.of(imageUrls.get(0)));
        }

        List<List<String>> pairs = new ArrayList<>();
        for (int index = 0; index < imageUrls.size() - 1; index++) {
            pairs.add(List.of(imageUrls.get(index), imageUrls.get(index + 1)));
        }
        return List.copyOf(pairs);
    }

    private List<String> collapseRepeatedPostures(List<String> frames) {
        List<String> canonical = new ArrayList<>();
        for (String frame : frames) {
            if (canonical.isEmpty() || !canonical.get(canonical.size() - 1).equals(frame)) {
                canonical.add(frame);
            }
        }
        // 같은 자세를 유지하는 구간은 영상으로 보간할 움직임이 아니므로 정적 guide로 축약한다.
        return List.copyOf(canonical);
    }

    private KoreanPhonemeUtil.HangulChar decomposeSingleSyllable(String syllable) {
        if (syllable == null || syllable.codePointCount(0, syllable.length()) != 1) {
            return null;
        }
        return KoreanPhonemeUtil.decompose(syllable.charAt(0));
    }

    private void appendRoleFrames(List<String> destination, String key, VideoType type) {
        List<String> mappedFrames = getImageFrames(key, type);
        if (mappedFrames.isEmpty()) {
            // null은 프레임 삭제가 아니라 이 구간에서 인접 자세를 유지해야 한다는 내부 표식이다.
            destination.add(null);
            return;
        }
        destination.addAll(mappedFrames);
    }

    private List<String> fillMissingFrames(List<String> frames) {
        if (frames.stream().noneMatch(value -> value != null && !value.isBlank())) {
            return List.of();
        }

        List<String> filled = new ArrayList<>(frames);
        for (int index = 0; index < filled.size(); index++) {
            if (filled.get(index) == null || filled.get(index).isBlank()) {
                filled.set(index, findNearestFrame(frames, index));
            }
        }
        return Collections.unmodifiableList(filled);
    }

    private String findNearestFrame(List<String> frames, int missingIndex) {
        for (int distance = 1; distance < frames.size(); distance++) {
            int left = missingIndex - distance;
            if (left >= 0 && frames.get(left) != null && !frames.get(left).isBlank()) {
                return frames.get(left);
            }
            int right = missingIndex + distance;
            if (right < frames.size() && frames.get(right) != null && !frames.get(right).isBlank()) {
                return frames.get(right);
            }
        }
        throw new IllegalStateException("가이드 프레임 보간 기준을 찾을 수 없음");
    }

    private String getGuideBaseUrl() {
        if (awsSettings == null
                || awsSettings.getS3() == null
                || isBlank(awsSettings.getS3().getBucket())
                || isBlank(awsSettings.getS3().getRegion())) {
            return S3_BASE_URL;
        }
        return String.format(
                "https://%s.s3.%s.amazonaws.com/guides",
                awsSettings.getS3().getBucket(),
                awsSettings.getS3().getRegion()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** JSON의 modality별 프레임 배열을 불변 값으로 보관한다. */
    private static final class SyllableMapping {
        private final List<String> mouthFrames;
        private final List<String> tongueFrames;

        private SyllableMapping(List<String> mouthFrames, List<String> tongueFrames) {
            this.mouthFrames = List.copyOf(mouthFrames);
            this.tongueFrames = List.copyOf(tongueFrames);
        }

        private List<String> frames(VideoType type) {
            return type == VideoType.MOUTH ? mouthFrames : tongueFrames;
        }
    }
}
