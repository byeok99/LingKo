package com.lingko.lingko.core.domain.evaluation.entity;

import com.lingko.lingko.core.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluation Log 상태를 영속화하고 불변 조건를 지키는 상태 전이를 소유한다.
 *
 * 어떤 서비스가 호출해도 동일한 규칙이 유지되어야 하는 동작이므로 데이터를 가진 엔티티에 배치했다.
 */
@Entity
@Table(
        name = "evaluation_log",
        indexes = {
                @Index(name = "idx_user_created", columnList = "user_idx, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EvaluationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_log_idx")
    private Long evaluationLogIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_idx", nullable = false)
    private User user;

    @Column(name = "original_word", nullable = false, length = 300)
    private String originalWord;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private PracticeSource source;

    @Column(name = "sentence_id")
    private Long sentenceId;

    @Column(name = "standard_pronunciation", nullable = false, length = 300)
    private String standardPronunciation;

    @Column(name = "recognized_text", length = 300)
    private String recognizedText;

    @Column(name = "accuracy_score", precision = 5, scale = 2)
    private BigDecimal accuracyScore;

    @Column(name = "fluency_score", precision = 5, scale = 2)
    private BigDecimal fluencyScore;

    @Column(name = "completeness_score", precision = 5, scale = 2)
    private BigDecimal completenessScore;

    @Column(name = "pronunciation_score", precision = 5, scale = 2)
    private BigDecimal pronunciationScore;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "evaluationLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EvaluationSyllable> syllableList = new ArrayList<>();

    @OneToMany(mappedBy = "evaluationLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EvaluationWord> wordList = new ArrayList<>();

    public void addSyllable(EvaluationSyllable syllable) {
        if (syllable == null) {
            throw new IllegalArgumentException("syllable must not be null");
        }
        // JPA cascade는 aggregate root에서 시작하지만 foreign key는 child가 소유하므로 양방향 관계를 함께 맞춘다.
        syllableList.add(syllable);
        syllable.setEvaluationLog(this);
    }

    public void addWord(EvaluationWord word) {
        if (word == null) {
            throw new IllegalArgumentException("word must not be null");
        }
        // 단어도 aggregate root와 양방향 관계를 같이 맞춰 cascade insert의 FK를 보장한다.
        wordList.add(word);
        word.setEvaluationLog(this);
    }

    /** 평가 문장이 추천 목록과 사용자 직접 입력 중 어디에서 시작됐는지 나타낸다. */
    public enum PracticeSource {
        RECOMMENDED,
        CUSTOM
    }
}
