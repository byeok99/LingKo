ALTER TABLE syllables
    ADD COLUMN mouth_mapping_version VARCHAR(32) NULL AFTER mouth_url;

ALTER TABLE syllables
    ADD COLUMN tongue_mapping_version VARCHAR(32) NULL AFTER tongue_url;
