package com.lingko.lingko.core.domain.practice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "practice_syllable")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PracticeSyllable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "practice_syllables_idx")
    private Long practiceSyllablesIdx;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_log_idx", nullable = false)
    private PracticeLog practiceLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllable_char", nullable = false)
    private Syllable syllable;

    @Column(name = "score", nullable = false)
    private Integer score;
}
