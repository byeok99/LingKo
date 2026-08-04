package com.lingko.lingko.core.domain.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 평가 시점의 단어 위치·텍스트·점수 snapshot을 한 번만 저장한다.
 *
 * 단어 점수를 음절 행에 복제하지 않아 점수의 신뢰 단위와 DB 소유 단위를 일치시킨다.
 */
@Entity
@Table(
        name = "evaluation_word",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_evaluation_word_log_position",
                columnNames = {"evaluation_log_idx", "position_no"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EvaluationWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_word_idx")
    private Long evaluationWordIdx;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_log_idx", nullable = false)
    private EvaluationLog evaluationLog;

    @Column(name = "position_no", nullable = false)
    private Integer positionNo;

    @Column(name = "word_text", nullable = false, length = 100)
    private String wordText;

    @Column(name = "score")
    private Integer score;
}
