package com.lingko.lingko.core.domain.sentence.repository;

import com.lingko.lingko.core.domain.sentence.entity.RecommendedSentence;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecommendedSentenceRepository extends JpaRepository<RecommendedSentence, Long> {
    List<RecommendedSentence> findByActiveTrueOrderBySortOrderAscSentenceIdAsc(Pageable pageable);

    List<RecommendedSentence> findByActiveTrueAndCategoryCodeOrderBySortOrderAscSentenceIdAsc(
            String categoryCode,
            Pageable pageable
    );

    Optional<RecommendedSentence> findBySentenceIdAndActiveTrue(Long sentenceId);
}
