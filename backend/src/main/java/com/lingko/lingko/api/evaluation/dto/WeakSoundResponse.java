package com.lingko.lingko.api.evaluation.dto;

/**
 * Home의 "Sound to fix" 타일과 Sound detail 머리말에 쓰는 음절 하나의 누적 성적이다.
 *
 * {@code averageScore}는 이 음절이 들어간 어절들의 점수를 모은 값이지, 음절 자체를 측정한
 * 점수가 아니다. 공급자가 한국어 음절 점수를 신뢰할 수 있게 주지 않아 DB에 음절 성적이 없다.
 * 같은 음절이 여러 어절에서 반복해 낮게 나오면 그 소리가 원인일 가능성이 높다는 추정이며,
 * 화면도 "이 음절이 든 연습들의 평균"으로 읽히게 표시해야 한다.
 *
 * {@code attemptCount}는 이 음절이 들어간 어절을 연습한 횟수다. 반올림은 여기서 끝내
 * 화면이 평균을 다시 계산하지 않는다.
 */
public record WeakSoundResponse(
        String text,
        String romanization,
        int averageScore,
        long attemptCount
) {
}
