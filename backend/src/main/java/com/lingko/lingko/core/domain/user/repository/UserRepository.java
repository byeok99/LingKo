package com.lingko.lingko.core.domain.user.repository;

import com.lingko.lingko.core.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * User 영속성 연산을 추상화한다.
 *
 * 도메인 서비스가 query와 저장 기술 세부사항에 의존하지 않도록 저장소 경계를 선택했다.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findBySocialIdAndSocialType(
            String socialId,
            User.SocialType socialType
    );

    /**
     * 사용자별 일일 resource 최초 생성처럼 child 행이 아직 없을 때 parent를 경쟁 조정 지점으로 사용한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.userIdx = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);
}
