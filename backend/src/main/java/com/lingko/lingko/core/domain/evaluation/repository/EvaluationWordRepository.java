package com.lingko.lingko.core.domain.evaluation.repository;

import com.lingko.lingko.core.domain.evaluation.dto.WeakWordAggregate;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationWord;
import org.springframework.data.domain.Pageable;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 평가 시점의 단어 점수 snapshot 영속성 연산을 제공한다. */
@Repository
public interface EvaluationWordRepository extends JpaRepository<EvaluationWord, Long> {

    /**
     * 사용자가 반복해서 틀리는 어절을 평균 점수가 낮은 순으로 집계한다.
     *
     * 음절 단위로 집계하지 않는 이유는 신뢰할 수 있는 점수의 최소 단위가 어절이기 때문이다.
     * 공급자 음절 점수는 한국어에서 신뢰할 수 없다고 판단해 저장하지 않는다.
     * 한 번만 나온 어절은 표본이 부족해 평균이 흔들리므로 최소 시도 횟수로 걸러낸다.
     */
    @Query("""
            select new com.lingko.lingko.core.domain.evaluation.dto.WeakWordAggregate(
                word.wordText,
                avg(word.score),
                count(word)
            )
            from EvaluationWord word
            where word.evaluationLog.user.userIdx = :userId
              and word.score is not null
            group by word.wordText
            having count(word) >= :minimumAttempts
            order by avg(word.score) asc, count(word) desc, word.wordText asc
            """)
    List<WeakWordAggregate> findWeakWords(
            @Param("userId") Long userId,
            @Param("minimumAttempts") long minimumAttempts,
            Pageable pageable
    );

    /**
     * 특정 어절을 연습한 과거 시도를 최신순으로 반환한다.
     *
     * 어절 행에서 상위 평가 log로 올라가야 문장 원문과 점수·시각을 함께 보여줄 수 있으므로
     * log를 함께 가져와 화면이 건별로 다시 조회하지 않게 한다.
     */
    @Query("""
            select word
            from EvaluationWord word
            join fetch word.evaluationLog log
            where log.user.userIdx = :userId
              and word.wordText = :wordText
            order by log.createdAt desc, word.evaluationWordIdx desc
            """)
    List<EvaluationWord> findPracticedByWord(
            @Param("userId") Long userId,
            @Param("wordText") String wordText,
            Pageable pageable
    );

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
