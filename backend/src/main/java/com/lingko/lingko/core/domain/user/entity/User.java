package com.lingko.lingko.core.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "target_level", nullable = false, length = 30)
    private LearningLevel targetLevel = LearningLevel.BEGINNER_2;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_login_at", nullable = false)
    private LocalDateTime lastLoginAt;

    public void updateOAuthProfile(String email, String name, String profileImageUrl) {
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    public void updateLearningPreferences(String displayLanguage, String nativeLanguage, LearningLevel targetLevel) {
        this.displayLanguage = displayLanguage;
        this.nativeLanguage = nativeLanguage;
        this.targetLevel = targetLevel;
    }

    public enum SocialType {
        GOOGLE, APPLE, KAKAO
    }

    public enum LearningLevel {
        BEGINNER_1,
        BEGINNER_2,
        INTERMEDIATE_1,
        INTERMEDIATE_2,
        ADVANCED
    }
}
