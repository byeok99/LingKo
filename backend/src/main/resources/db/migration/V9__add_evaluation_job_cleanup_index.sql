CREATE INDEX idx_evaluation_jobs_cleanup
    ON evaluation_jobs (status, completed_at);
