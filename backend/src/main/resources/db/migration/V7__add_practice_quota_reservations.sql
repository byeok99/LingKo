ALTER TABLE daily_practice_quota
    ADD COLUMN free_reserved INT NOT NULL DEFAULT 0 AFTER free_used;

ALTER TABLE daily_practice_quota
    ADD COLUMN rewarded_reserved INT NOT NULL DEFAULT 0 AFTER rewarded_available;
