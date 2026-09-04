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

    /**
     * 단어 단위 점수 도입 이후 음절은 guide-only 단위이므로 신규 기록에서는 항상 null이다.
     *
     * V15 이전 기록에는 숫자가 남아 있지만 한국어 음절 점수는 신뢰할 수 없다고 판단해
     * 조회 계층에서 노출하지 않는다. 과거 데이터 호환을 위해 column만 nullable로 유지한다.
     */
    @Column(name = "score")
    private Integer score;

    @Column(name = "position_no", nullable = false)
    private Integer positionNo;

    /** 신규 기록의 음절을 상위 단어 위치에 연결하며 과거 기록은 nullable로 호환한다. */
    @Column(name = "word_position")
    private Integer wordPosition;

    @Column(name = "feedback", length = 500)
    private String feedback;

    @Column(name = "mouth_guide_url", length = 500)
    private String mouthGuideUrl;

    @Column(name = "tongue_guide_url", length = 500)
    private String tongueGuideUrl;
}
