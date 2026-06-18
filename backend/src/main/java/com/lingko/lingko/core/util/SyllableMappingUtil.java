package com.lingko.lingko.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.core.config.AwsSettings;
import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

/**
 * syllable_mapping.json 로더
 */
@Slf4j
@Component
public class SyllableMappingUtil {

    private static final String S3_BASE_URL = "https://lingko.s3.ap-northeast-2.amazonaws.com/guides";

    private final AwsSettings awsSettings;
    private Map<String, SyllableMapping> mappingTable = new HashMap<>();

    public SyllableMappingUtil(AwsSettings awsSettings) {
        this.awsSettings = awsSettings;
    }

    SyllableMappingUtil() {
        this(null);
    }
    
    @PostConstruct
    public void loadMapping() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ClassPathResource resource = new ClassPathResource("syllable_mapping.json");
            
            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> rawData = mapper.readValue(
                resource.getInputStream(),
                Map.class
            );
            
            for (Map.Entry<String, Map<String, String>> entry : rawData.entrySet()) {
                String jamo = entry.getKey();
                Map<String, String> urls = entry.getValue();
                
                mappingTable.put(jamo, new SyllableMapping(
                    urls.getOrDefault("mouth_url", ""),
                    urls.getOrDefault("tongue_url", "")
                ));
            }
            
            log.info("syllable_mapping.json 로드 완료: {}개", mappingTable.size());
            
        } catch (IOException e) {
            log.error("syllable_mapping.json 로드 실패", e);
            throw new RuntimeException("Failed to load syllable mapping", e);
        }
    }
    
    public SyllableMapping getMapping(String jamo) {
        return mappingTable.getOrDefault(jamo, new SyllableMapping("", ""));
    }

    /**
     * 자모의 이미지 URL 가져오기
     *
     * @param phoneme 자모
     * @param type VideoType
     * @return S3 이미지 URL (없으면 null)
     */
    public String getImageUrl(String phoneme, VideoType type) {
        SyllableMapping mapping = getMapping(phoneme);

        String filename = type == VideoType.MOUTH ? mapping.getMouthUrl() : mapping.getTongueUrl();

        if (filename == null || filename.isEmpty()) {
            return null;
        }

        // guides/mouth/bilabial-consonants.png
        String folder = type == VideoType.MOUTH ? "mouth" : "tongue";
        return String.format("%s/%s/%s", getGuideBaseUrl(), folder, filename);
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

    /**
     * 자모 리스트의 이미지 URL 리스트
     *
     * @param phonemes 자모 리스트
     * @param type VideoType
     * @return 이미지 URL 리스트
     */
    public List<String> getImageUrls(List<String> phonemes, VideoType type) {
        List<String> urls = new ArrayList<>();

        for (String phoneme : phonemes) {
            String url = getImageUrl(phoneme, type);
            if(url != null) urls.add(url);
        }

        return urls;
    }

    /**
     * 프레임 쌍 생성 (Frame Interpolation용)
     *
     * @param phonemes 자모 리스트
     * @param type VideoType
     * @return [[url1, url2], [url2, url3], ...]
     */
    public List<List<String>> createFramePairs(List<String> phonemes, VideoType type) {
        List<String> imageUrls = getImageUrls(phonemes, type);

        // 이미지 없음
        if (imageUrls.isEmpty()) {
            return Collections.emptyList();
        }

        // 단일 이미지 (정적 이미지)
        if (imageUrls.size() == 1) {
            return List.of(List.of(imageUrls.get(0)));
        }

        // 프레임 쌍 생성
        List<List<String>> framePairs = new ArrayList<>();
        for (int i = 0; i < imageUrls.size() - 1; i++) {
            framePairs.add(List.of(imageUrls.get(i), imageUrls.get(i + 1)));
        }

        return framePairs;
    }

    @Getter
    public static class SyllableMapping {
        private final String mouthUrl;
        private final String tongueUrl;
        
        public SyllableMapping(String mouthUrl, String tongueUrl) {
            this.mouthUrl = mouthUrl;
            this.tongueUrl = tongueUrl;
        }
        
        public boolean hasMouth() {
            return mouthUrl != null && !mouthUrl.isEmpty();
        }
        
        public boolean hasTongue() {
            return tongueUrl != null && !tongueUrl.isEmpty();
        }
    }
}
