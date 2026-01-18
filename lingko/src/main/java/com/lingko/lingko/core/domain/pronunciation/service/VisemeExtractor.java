package com.lingko.lingko.core.domain.pronunciation.service;

import com.lingko.lingko.core.util.KoreanPhonemeUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 혀/입 모양 변화 분석 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisemeExtractor {
    
    private final SyllableMappingLoader mappingLoader;
    
    // 변이음 구분용 (ㅅ, ㅆ + i계열 모음)
    private static final Set<String> PALATAL_VOWELS = Set.of(
        "ㅣ", "ㅑ", "ㅒ", "ㅕ", "ㅖ", "ㅛ", "ㅠ"
    );
    
    // 혀가 고정되거나 거의 사용되지 않는 초성
    private static final Set<String> TONGUE_STATIC_ONSETS = Set.of(
        "ㅁ", "ㅂ", "ㅃ", "ㅍ", "ㅇ"
    );
    
    /**
     * 혀 움직임 작업 생성
     */
    public List<TongueJob> makeTongueJobs(String syllable) {
        List<TongueJob> jobs = new ArrayList<>();
        
        for (char ch : syllable.toCharArray()) {
            if (!isKorean(ch)) continue;
            
            KoreanPhonemeUtil.HangulChar hc = KoreanPhonemeUtil.decompose(ch);
            if (hc == null) continue;
            
            String chosung = hc.getChosung();
            String jungsung = hc.getJungsung();
            String jongsung = hc.getJongsung();
            
            // [1] 초성 → 중성
            String frame1 = getTonguePath(chosung, jungsung, "초성");
            String frame2 = getTonguePath(jungsung, null, "중성");
            
            if (frame2 == null || frame2.isEmpty()) {
                continue;
            }
            
            // 혀 고정 자음 또는 프레임이 동일 → 단독 이미지
            if (TONGUE_STATIC_ONSETS.contains(chosung) || 
                frame1 == null || frame1.isEmpty() || frame1.equals(frame2)) {
                
                jobs.add(TongueJob.builder()
                    .letter(String.valueOf(ch))
                    .frame(frame2)
                    .segment("단독")
                    .type("image")
                    .output(extractFilename(frame2))
                    .build());
            } else {
                jobs.add(TongueJob.builder()
                    .letter(String.valueOf(ch))
                    .frame1(frame1)
                    .frame2(frame2)
                    .segment("초성중성")
                    .type("video")
                    .output(extractFilename(frame1) + "_" + extractFilename(frame2) + ".mp4")
                    .build());
            }
            
            // [2] 중성 → 종성
            if (!jongsung.isEmpty()) {
                String frame3 = getTonguePath(jongsung, null, "종성");
                
                if (frame2 != null && !frame2.isEmpty() && 
                    frame3 != null && !frame3.isEmpty()) {
                    
                    if (frame2.equals(frame3)) {
                        jobs.add(TongueJob.builder()
                            .letter(String.valueOf(ch))
                            .frame(frame2)
                            .segment("중성종성")
                            .type("image")
                            .output(extractFilename(frame2))
                            .build());
                    } else {
                        jobs.add(TongueJob.builder()
                            .letter(String.valueOf(ch))
                            .frame1(frame2)
                            .frame2(frame3)
                            .segment("중성종성")
                            .type("video")
                            .output(extractFilename(frame2) + "_" + extractFilename(frame3) + ".mp4")
                            .build());
                    }
                }
            }
        }
        
        return jobs;
    }
    
    /**
     * 입 모양 시퀀스 추출
     */
    public List<LetterLipsSequence> extractLipsSequence(String word) {
        List<LetterLipsSequence> results = new ArrayList<>();
        
        for (char ch : word.toCharArray()) {
            if (!isKorean(ch)) continue;
            
            KoreanPhonemeUtil.HangulChar hc = KoreanPhonemeUtil.decompose(ch);
            if (hc == null) continue;
            
            String chosung = hc.getChosung();
            String jungsung = hc.getJungsung();
            String jongsung = hc.getJongsung();
            
            List<String> sequence = new ArrayList<>();
            
            // 초성 (ㅎ 제외)
            if (!chosung.equals("ㅎ")) {
                String lipsPath = getLipsPath(chosung, jungsung, "초성");
                if (lipsPath != null && !lipsPath.isEmpty()) {
                    sequence.add(lipsPath);
                }
            }
            
            // 중성
            String lipsPath = getLipsPath(jungsung, null, "중성");
            if (lipsPath != null && !lipsPath.isEmpty()) {
                sequence.add(lipsPath);
            }
            
            // 종성 (ㅎ 제외)
            if (!jongsung.isEmpty() && !jongsung.equals("ㅎ")) {
                lipsPath = getLipsPath(jongsung, null, "종성");
                if (lipsPath != null && !lipsPath.isEmpty()) {
                    sequence.add(lipsPath);
                }
            }
            
            results.add(LetterLipsSequence.builder()
                .letter(String.valueOf(ch))
                .sequence(sequence)
                .build());
        }
        
        return results;
    }
    
    /**
     * 입 모양 작업 생성
     */
    public List<LipsJob> makeLipsJobs(List<String> sequence) {
        List<LipsJob> jobs = new ArrayList<>();
        
        if (sequence.isEmpty()) return jobs;
        
        // 1개만: 단독 프레임
        if (sequence.size() == 1) {
            String frame = sequence.get(0);
            if (!frame.isEmpty()) {
                jobs.add(LipsJob.builder()
                    .frame1(frame)
                    .frame2(null)
                    .segment("단독")
                    .output(extractFilename(frame) + ".png")
                    .build());
            }
            return jobs;
        }
        
        // 2개 이상: 인접 쌍마다 job
        for (int i = 0; i < sequence.size() - 1; i++) {
            String frame1 = sequence.get(i);
            String frame2 = sequence.get(i + 1);
            
            if (!frame1.isEmpty() && !frame2.isEmpty() && !frame1.equals(frame2)) {
                jobs.add(LipsJob.builder()
                    .frame1(frame1)
                    .frame2(frame2)
                    .segment(i + "to" + (i + 1))
                    .output(extractFilename(frame1) + "_" + extractFilename(frame2) + ".png")
                    .build());
            }
        }
        
        return jobs;
    }
    
    /**
     * 혀 이미지 경로 가져오기
     */
    private String getTonguePath(String jamo, String vowel, String position) {
        if (jamo == null || jamo.isEmpty()) return null;
        
        // ㅎ은 생략
        if (jamo.equals("ㅎ") && position.equals("초성")) {
            return null;
        }
        
        // ㅅ, ㅆ 변이음 처리
        if ((jamo.equals("ㅅ") || jamo.equals("ㅆ")) && position.equals("초성")) {
            String key = getSibilantKey(jamo, vowel);
            return mappingLoader.getMapping(key).getTongueUrl();
        }
        
        return mappingLoader.getMapping(jamo).getTongueUrl();
    }
    
    /**
     * 입 이미지 경로 가져오기
     */
    private String getLipsPath(String jamo, String vowel, String position) {
        if (jamo == null || jamo.isEmpty()) return null;
        
        // ㅅ, ㅆ 변이음 처리
        if ((jamo.equals("ㅅ") || jamo.equals("ㅆ")) && position.equals("초성")) {
            String key = getSibilantKey(jamo, vowel);
            return mappingLoader.getMapping(key).getLipsUrl();
        }
        
        return mappingLoader.getMapping(jamo).getLipsUrl();
    }
    
    /**
     * ㅅ/ㅆ 변이음 키 생성
     */
    private String getSibilantKey(String onset, String vowel) {
        if (vowel != null && PALATAL_VOWELS.contains(vowel)) {
            return "변이음" + onset;
        }
        return onset;
    }
    
    /**
     * 파일명 추출
     */
    private String extractFilename(String path) {
        if (path == null || path.isEmpty()) return "";
        
        // 확장자 제거
        String filename = path;
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            filename = filename.substring(0, dotIndex);
        }
        
        return filename;
    }
    
    /**
     * 한글 여부 확인
     */
    private boolean isKorean(char ch) {
        return ch >= 0xAC00 && ch <= 0xD7A3;
    }
    
    // ===== DTO 클래스 =====
    
    @Getter
    @Builder
    public static class TongueJob {
        private String letter;
        private String frame;      // 단독 이미지용
        private String frame1;     // 영상용 시작 프레임
        private String frame2;     // 영상용 끝 프레임
        private String segment;    // "단독", "초성중성", "중성종성"
        private String type;       // "image" or "video"
        private String output;     // 출력 파일명
    }
    
    @Getter
    @Builder
    public static class LipsJob {
        private String frame1;
        private String frame2;
        private String segment;
        private String output;
    }
    
    @Getter
    @Builder
    public static class LetterLipsSequence {
        private String letter;
        private List<String> sequence;
    }
}
