package com.lingko.lingko.core.domain.evaluation.entity;

import com.lingko.lingko.core.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    public void addSyllable(EvaluationSyllable syllable) {
        if (syllable == null) {
            throw new IllegalArgumentException("syllable must not be null");
        }
        syllableList.add(syllable);
        syllable.setEvaluationLog(this);
    }

    public enum PracticeSource {
        RECOMMENDED,
        CUSTOM
    }
}
