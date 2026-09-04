ALTER TABLE daily_practice_quota
    ADD COLUMN next_refill_at DATETIME(6) NULL AFTER rewarded_reserved;
