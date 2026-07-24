package com.lingko.lingko.core.domain.evaluation.repository;

import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Evaluation Log 영속성 연산을 추상화한다.
 *
 * 도메인 서비스가 query와 저장 기술 세부사항에 의존하지 않도록 저장소 경계를 선택했다.
 */
@Repository
public interface EvaluationLogRepository extends JpaRepository<EvaluationLog, Long> {

    Page<EvaluationLog> findByUser_UserIdxOrderByCreatedAtDesc(
            Long userIdx,
            Pageable pageable
    );

    @Query("select max(e.score) from EvaluationLog e where e.user.userIdx = :userIdx")
    Integer findBestScoreByUserIdx(Long userIdx);
}
