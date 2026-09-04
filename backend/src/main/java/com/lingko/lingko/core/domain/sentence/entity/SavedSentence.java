package com.lingko.lingko.core.domain.sentence.entity;

import com.lingko.lingko.core.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자가 다시 연습하려고 저장해 둔 추천 문장이다.
 *
 * 문장 본문을 복사하지 않고 참조만 남긴다. 추천 문장이 수정되면 저장 목록도 함께 최신 내용을
 * 보여주는 것이 학습자에게 맞고, 같은 문장을 여러 사용자가 저장해도 본문이 중복되지 않는다.
 */
@Entity
@Table(
        name = "saved_sentence",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_saved_sentence_user_sentence",
                columnNames = {"user_idx", "sentence_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SavedSentence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "saved_sentence_idx")
    private Long savedSentenceIdx;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_idx", nullable = false)
    private User user;

    /**
     * 저장한 추천 문장의 식별자다.
     *
     * 연관 엔티티 대신 식별자만 두는 이유는 저장 토글이 문장 본문을 필요로 하지 않아서다.
     * 목록 조회에서만 문장을 함께 읽는다.
     */
    @Column(name = "sentence_id", nullable = false)
    private Long sentenceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
