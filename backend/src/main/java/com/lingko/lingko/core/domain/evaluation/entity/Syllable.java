package com.lingko.lingko.core.domain.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="syllables")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Syllable {
    @Id
    @Column(name="syllable_char", length=10)
    private String syllableChar;

    @Column(name="mouth_url", length=500)
    private String mouthUrl;

    @Column(name="tongue_url", length=500)
    private String tongueUrl;
}
