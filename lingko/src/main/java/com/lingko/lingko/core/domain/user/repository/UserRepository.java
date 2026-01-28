package com.lingko.lingko.core.domain.user.repository;

import com.lingko.lingko.core.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findBySocialIdAndSocialType(
            String socialId,
            User.SocialType socialType
    );
}
