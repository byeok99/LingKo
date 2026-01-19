package com.lingko.lingko.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * syllable_mapping.json 로더
 */
@Slf4j
@Component
public class SyllableMappingUtil {
    
    private Map<String, SyllableMapping> mappingTable = new HashMap<>();
    
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
                    urls.getOrDefault("lips_url", ""),
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
    
    @Getter
    public static class SyllableMapping {
        private final String lipsUrl;
        private final String tongueUrl;
        
        public SyllableMapping(String lipsUrl, String tongueUrl) {
            this.lipsUrl = lipsUrl;
            this.tongueUrl = tongueUrl;
        }
        
        public boolean hasLips() {
            return lipsUrl != null && !lipsUrl.isEmpty();
        }
        
        public boolean hasTongue() {
            return tongueUrl != null && !tongueUrl.isEmpty();
        }
    }
}
