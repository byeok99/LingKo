package com.lingko.lingko.core.domain.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "evaluation_syllable")
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
}
