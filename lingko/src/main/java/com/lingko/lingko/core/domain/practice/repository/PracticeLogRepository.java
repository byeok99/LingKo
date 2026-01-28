package com.lingko.lingko.core.domain.practice.repository;

import com.lingko.lingko.core.domain.practice.entity.PracticeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PracticeLogRepository extends JpaRepository<PracticeLog, Long> {

    Page<PracticeLog> findByUser_UserIdxOrderByCreatedAtDesc(
            Long userIdx,
            Pageable pageable
    );
}
