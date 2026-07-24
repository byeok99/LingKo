package com.lingko.lingko.core.domain.sentence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Recommended Sentence 상태를 영속화하고 불변 조건를 지키는 상태 전이를 소유한다.
 *
 * 어떤 서비스가 호출해도 동일한 규칙이 유지되어야 하는 동작이므로 데이터를 가진 엔티티에 배치했다.
 */
@Entity
@Table(name = "recommended_sentences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RecommendedSentence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sentence_id")
    private Long sentenceId;

    @Column(name = "original_text", nullable = false, length = 120)
    private String originalText;

    @Column(name = "standard_pronunciation", nullable = false, length = 120)
    private String standardPronunciation;

    @Column(name = "translation", nullable = false, length = 255)
    private String translation;

    @Column(name = "level_label", nullable = false, length = 50)
    private String levelLabel;

    @Column(name = "category_code", nullable = false, length = 50)
    private String categoryCode;

    @Column(name = "category_label", nullable = false, length = 50)
    private String categoryLabel;

    @Column(name = "learning_point", nullable = false, length = 255)
    private String learningPoint;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
