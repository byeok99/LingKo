package com.lingko.lingko.core.domain.user;

import com.lingko.lingko.api.user.dto.UserPreferencesResponse;
import com.lingko.lingko.api.user.dto.UserPreferencesUpdateRequest;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import com.lingko.lingko.core.domain.user.service.UserPreferencesService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * User Preferences 서비스 Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem=user_preferences;MODE=MySQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@Import(UserPreferencesService.class)
class UserPreferencesServiceTest {

    @Autowired
    private UserPreferencesService preferencesService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("신규 사용자는 기본 학습 설정을 가진다")
    void newUserHasDefaultPreferences() {
        User user = userRepository.save(User.builder()
                .socialId("google-sub-123")
                .socialType(User.SocialType.GOOGLE)
                .email("user@example.com")
                .name("LingKo User")
                .build());

        UserPreferencesResponse response = preferencesService.findPreferences(user.getUserIdx());

        assertThat(response.nativeLanguage()).isEqualTo("en");
    }

    @Test
    @DisplayName("사용자 학습 설정을 저장한다")
    void updatePreferences() {
        User user = userRepository.save(User.builder()
                .socialId("google-sub-123")
                .socialType(User.SocialType.GOOGLE)
                .email("user@example.com")
                .name("LingKo User")
                .build());

        UserPreferencesResponse response = preferencesService.updatePreferences(
                user.getUserIdx(),
                new UserPreferencesUpdateRequest("ja")
        );
        entityManager.flush();
        entityManager.clear();

        User updated = userRepository.findById(user.getUserIdx()).orElseThrow();
        assertThat(response.nativeLanguage()).isEqualTo("ja");
        assertThat(updated.getNativeLanguage()).isEqualTo("ja");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 설정 조회는 인증 실패로 처리한다")
    void missingUserIsRejected() {
        assertThatThrownBy(() -> preferencesService.findPreferences(404L))
                .isInstanceOf(AuthException.class);
    }
}
