package com.lingko.lingko.core.domain.practice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "practice_log",
        indexes = {
                @Index(name = "idx_user_created", columnList = "user_idx, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PracticeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "practice_log_idx")
    private Long practiceLogIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_idx", nullable = false)
    private User user;

    @Column(name = "original_word", nullable = false, length = 50)
    private String originalWord;

    @Column(name = "score", nullable = false)
    private Integer score;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "practiceLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PracticeSyllable> syllableList = new ArrayList<>();

    public void addSyllable(PracticeSyllable syllable) {
        syllable.add(syllable);
        syllable.setPracticeLog(this);
    }
}
