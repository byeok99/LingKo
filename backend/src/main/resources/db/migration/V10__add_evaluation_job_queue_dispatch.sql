ALTER TABLE evaluation_jobs
    ADD COLUMN enqueued_at DATETIME(6);

CREATE INDEX idx_evaluation_jobs_dispatch
    ON evaluation_jobs (status, next_attempt_at, enqueued_at, created_at);
