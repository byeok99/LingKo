package com.lingko.lingko.core.domain.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Syllable 상태를 영속화하고 불변 조건를 지키는 상태 전이를 소유한다.
 *
 * 어떤 서비스가 호출해도 동일한 규칙이 유지되어야 하는 동작이므로 데이터를 가진 엔티티에 배치했다.
 */
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
