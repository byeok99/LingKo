DROP INDEX idx_evaluation_jobs_dispatch ON evaluation_jobs;

ALTER TABLE evaluation_jobs
    DROP COLUMN enqueued_at;
