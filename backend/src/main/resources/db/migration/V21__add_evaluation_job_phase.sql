-- 퍼센트를 추측하지 않고 Worker가 완료한 실제 처리 경계를 앱 polling 응답에 제공한다.
ALTER TABLE evaluation_jobs
    ADD COLUMN phase VARCHAR(30) NOT NULL DEFAULT 'QUEUED' AFTER status;

-- 배포 중 이미 실행 중인 작업은 최소한 음성 분석 단계로 표시하고 다음 Worker 갱신부터 정확한 phase를 사용한다.
UPDATE evaluation_jobs
SET phase = CASE
    WHEN status = 'PROCESSING' THEN 'ANALYZING_SPEECH'
    WHEN status IN ('SUCCEEDED', 'FAILED') THEN 'FINALIZING'
    ELSE 'QUEUED'
END;
