package com.lingko.lingko.core.domain.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Evaluation Syllable 상태를 영속화하고 불변 조건를 지키는 상태 전이를 소유한다.
 *
 * 어떤 서비스가 호출해도 동일한 규칙이 유지되어야 하는 동작이므로 데이터를 가진 엔티티에 배치했다.
 */
@Entity
@Table(
        name = "evaluation_syllable",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_evaluation_syllable_log_position",
                        columnNames = {"evaluation_log_idx", "position_no"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EvaluationSyllable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_syllables_idx")
    private Long practiceSyllablesIdx;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_log_idx", nullable = false)
    private EvaluationLog evaluationLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllable_char", nullable = false)
    private Syllable syllable;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "position_no", nullable = false)
    private Integer positionNo;

    @Column(name = "feedback", length = 500)
    private String feedback;

    @Column(name = "mouth_guide_url", length = 500)
    private String mouthGuideUrl;

    @Column(name = "tongue_guide_url", length = 500)
    private String tongueGuideUrl;
}
