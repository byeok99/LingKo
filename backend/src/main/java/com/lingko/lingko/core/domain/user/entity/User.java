package com.lingko.lingko.core.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * User 상태를 영속화하고 불변 조건를 지키는 상태 전이를 소유한다.
 *
 * 어떤 서비스가 호출해도 동일한 규칙이 유지되어야 하는 동작이므로 데이터를 가진 엔티티에 배치했다.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"social_id", "social_type"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_idx")
    private Long userIdx;

    @Column(name = "social_id", nullable = false, length = 255)
    private String socialId;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type",nullable = false)
    private SocialType socialType;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_login_at", nullable = false)
    private LocalDateTime lastLoginAt;

    /**
     * 공급자가 이번 로그인에서 실제로 반환한 profile 값만 최신 snapshot에 반영한다.
     *
     * <p>null은 값을 지우라는 요청이 아니라 공급자가 이번 응답에서 제공하지 않았다는 뜻이다.
     * 특히 Apple 재로그인은 이름을 생략하므로 기존 이름을 보존해야 한다.</p>
     */
    public void updateOAuthProfile(String email, String name, String profileImageUrl) {
        // Apple은 이름을 최초 승인 때만 전달하므로 null 응답이 기존 profile snapshot을 지우지 않게 한다.
        if (email != null) {
            this.email = email;
        }
        if (name != null) {
            this.name = name;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    /** 사용자 계정을 외부 공급자 subject와 함께 유일하게 식별하는 공급자 종류다. */
    public enum SocialType {
        GOOGLE, APPLE, KAKAO
    }
}
