package com.lingko.lingko.core.domain.evaluation.repository;

import com.lingko.lingko.core.domain.evaluation.entity.EvaluationWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 평가 시점의 단어 점수 snapshot 영속성 연산을 제공한다. */
@Repository
public interface EvaluationWordRepository extends JpaRepository<EvaluationWord, Long> {

    /** 회원 탈퇴 시 평가 log보다 먼저 사용자 소유 단어 snapshot을 제거한다. */
    @Modifying
    @Query("""
            delete from EvaluationWord word
            where word.evaluationLog.evaluationLogIdx in (
                select log.evaluationLogIdx
                from EvaluationLog log
                where log.user.userIdx = :userId
            )
            """)
    int deleteAllByUserId(@Param("userId") Long userId);
}
