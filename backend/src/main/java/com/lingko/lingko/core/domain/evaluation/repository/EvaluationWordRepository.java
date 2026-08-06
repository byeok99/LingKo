package com.lingko.lingko.core.domain.evaluation.repository;

import com.lingko.lingko.core.domain.evaluation.dto.WordScoreAggregate;
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
     * 사용자가 점수를 받은 어절을 하나씩 평균·횟수로 집계한다.
     *
     * 여기서 음절까지 내려가지 않는 이유는 측정된 점수의 최소 단위가 어절이기 때문이다.
     * 공급자 음절 점수는 한국어에서 신뢰할 수 없다고 판단해 저장하지 않으므로
     * ({@code evaluation_syllable.score}는 항상 NULL) DB에는 음절 성적 자체가 없다.
     * 취약 음절은 이 어절 집계를 글자 단위로 다시 나눠 service 계층에서 만든다.
     *
     * 행이 아니라 서로 다른 어절 수만큼만 돌려주므로 사용자 이력이 길어져도 결과가 완만하게 는다.
     * 그래도 상한이 없으면 메모리 사용이 이력에 비례하므로 호출 측이 Pageable로 잘라 쓴다.
     */
    @Query("""
            select new com.lingko.lingko.core.domain.evaluation.dto.WordScoreAggregate(
                word.wordText,
                avg(word.score),
                count(word)
            )
            from EvaluationWord word
            where word.evaluationLog.user.userIdx = :userId
              and word.score is not null
            group by word.wordText
            order by avg(word.score) asc, count(word) desc, word.wordText asc
            """)
    List<WordScoreAggregate> findScoredWordAggregates(
            @Param("userId") Long userId,
            Pageable pageable
    );

    /**
     * 특정 음절이 들어간 어절을 연습한 과거 시도를 최신순으로 반환한다.
     *
     * 어절 전체가 아니라 부분 일치로 찾는 이유는 화면의 단위가 음절이기 때문이다.
     * 조회 대상이 한 글자이므로 like 앞뒤 wildcard를 쓸 수밖에 없고 index를 타지 못한다.
     * 사용자 한 명의 어절 snapshot으로 범위가 좁혀져 있어 감당 가능한 비용으로 본다.
     *
     * 어절 행에서 상위 평가 log로 올라가야 문장 원문과 점수·시각을 함께 보여줄 수 있으므로
     * log를 함께 가져와 화면이 건별로 다시 조회하지 않게 한다.
     */
    @Query("""
            select word
            from EvaluationWord word
            join fetch word.evaluationLog log
            where log.user.userIdx = :userId
              and word.wordText like concat('%', :character, '%')
            order by log.createdAt desc, word.evaluationWordIdx desc
            """)
    List<EvaluationWord> findPracticedByCharacter(
            @Param("userId") Long userId,
            @Param("character") String character,
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
