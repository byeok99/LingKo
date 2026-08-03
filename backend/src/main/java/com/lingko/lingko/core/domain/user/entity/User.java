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

    @Builder.Default
    @Column(name = "display_language", nullable = false, length = 20)
    private String displayLanguage = "en";

    @Builder.Default
    @Column(name = "native_language", nullable = false, length = 20)
    private String nativeLanguage = "en";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_login_at", nullable = false)
    private LocalDateTime lastLoginAt;

    public void updateOAuthProfile(String email, String name, String profileImageUrl) {
        // provider profile field는 로그인 시 갱신하는 시점 데이터이며 로컬 학습 설정은 변경하지 않는다.
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    public void updateLanguagePreferences(String displayLanguage, String nativeLanguage) {
        // 요청이 언어 설정 일부만 적용하지 않도록 표시·모국어 설정을 함께 갱신한다.
        this.displayLanguage = displayLanguage;
        this.nativeLanguage = nativeLanguage;
    }

    public enum SocialType {
        GOOGLE, APPLE, KAKAO
    }
}
