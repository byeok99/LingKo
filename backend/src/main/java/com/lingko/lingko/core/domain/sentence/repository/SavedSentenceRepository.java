package com.lingko.lingko.core.domain.sentence.repository;

import com.lingko.lingko.core.domain.sentence.entity.SavedSentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 저장한 문장의 소유자 기준 조회·토글 연산을 제공한다. */
@Repository
public interface SavedSentenceRepository extends JpaRepository<SavedSentence, Long> {

    /** 목록은 최근 저장한 것부터 보여준다. */
    List<SavedSentence> findByUserUserIdxOrderByCreatedAtDescSavedSentenceIdxDesc(Long userId);

    boolean existsByUserUserIdxAndSentenceId(Long userId, Long sentenceId);

    /** 저장 해제는 존재 여부를 먼저 묻지 않고 한 번의 삭제로 처리해 경쟁 조건을 줄인다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from SavedSentence saved
            where saved.user.userIdx = :userId
              and saved.sentenceId = :sentenceId
            """)
    int deleteByUserIdAndSentenceId(
            @Param("userId") Long userId,
            @Param("sentenceId") Long sentenceId
    );

    /** 회원 탈퇴 시 사용자 소유 저장 목록을 제거한다. */
    @Modifying
    @Query("delete from SavedSentence saved where saved.user.userIdx = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    /** Home 목록이 문장마다 저장 여부를 묻지 않도록 한 번에 조회한다. */
    @Query("""
            select saved.sentenceId
            from SavedSentence saved
            where saved.user.userIdx = :userId
            """)
    List<Long> findSavedSentenceIds(@Param("userId") Long userId);
}
