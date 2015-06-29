USE starexec;

ALTER TABLE jobs ADD suppress_timestamp BOOLEAN NOT NULL DEFAULT FALSE;
