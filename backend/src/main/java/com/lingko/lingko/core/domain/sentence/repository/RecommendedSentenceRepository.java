package com.lingko.lingko.core.domain.sentence.repository;

import com.lingko.lingko.core.domain.sentence.entity.RecommendedSentence;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Recommended Sentence 영속성 연산을 추상화한다.
 *
 * 도메인 서비스가 query와 저장 기술 세부사항에 의존하지 않도록 저장소 경계를 선택했다.
 */
public interface RecommendedSentenceRepository extends JpaRepository<RecommendedSentence, Long> {
    List<RecommendedSentence> findByActiveTrueOrderBySortOrderAscSentenceIdAsc(Pageable pageable);

    List<RecommendedSentence> findByActiveTrueAndCategoryCodeOrderBySortOrderAscSentenceIdAsc(
            String categoryCode,
            Pageable pageable
    );

    Optional<RecommendedSentence> findBySentenceIdAndActiveTrue(Long sentenceId);

    /**
     * 특정 어절이 들어간 추천 문장 중 아직 연습하지 않은 것을 반환한다.
     *
     * 이미 연습한 문장을 다시 제안하면 Practiced 목록과 겹쳐 같은 문장이 두 번 보인다.
     * 추천 문장은 표준 발음을 저장하지 않고 조회 시 변환하므로(V12) 원문만 대상으로 찾는다.
     * 표기가 달라 놓치는 후보는 있을 수 있으나, 잘못된 후보를 올리는 것보다 낫다.
     */
    @Query("""
            select sentence
            from RecommendedSentence sentence
            where sentence.active = true
              and sentence.originalText like %:wordText%
              and sentence.sentenceId not in (
                  select log.sentenceId
                  from EvaluationLog log
                  where log.user.userIdx = :userId
                    and log.sentenceId is not null
              )
            order by sentence.sortOrder asc, sentence.sentenceId asc
            """)
    List<RecommendedSentence> findUnpracticedByWord(
            @Param("userId") Long userId,
            @Param("wordText") String wordText,
            Pageable pageable
    );
}
