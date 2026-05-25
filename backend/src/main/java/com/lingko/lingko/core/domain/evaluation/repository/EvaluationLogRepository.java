package com.lingko.lingko.core.domain.evaluation.repository;

import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluationLogRepository extends JpaRepository<EvaluationLog, Long> {

    Page<EvaluationLog> findByUser_UserIdxOrderByCreatedAtDesc(
            Long userIdx,
            Pageable pageable
    );
}
