package com.lingko.lingko.core.domain.evaluation.repository;

import com.lingko.lingko.core.domain.evaluation.entity.EvaluationSyllable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Evaluation Syllable 영속성 연산을 추상화한다.
 *
 * 도메인 서비스가 query와 저장 기술 세부사항에 의존하지 않도록 저장소 경계를 선택했다.
 */
@Repository
public interface EvaluationSyllableRepository extends JpaRepository<EvaluationSyllable, Long> {

    @Modifying
    @Query("""
            delete from EvaluationSyllable syllable
            where syllable.evaluationLog.evaluationLogIdx in (
                select log.evaluationLogIdx
                from EvaluationLog log
                where log.user.userIdx = :userId
            )
            """)
    int deleteAllByUserId(@Param("userId") Long userId);
}
