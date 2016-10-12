USE starexec;
ALTER TABLE jobs ADD is_high_priority BOOLEAN NOT NULL DEFAULT FALSE;
