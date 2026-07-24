package com.lingko.lingko.core.domain.sentence.repository;

import com.lingko.lingko.core.domain.sentence.entity.RecommendedSentence;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
