USE starexec;

ALTER TABLE jobs
ADD COLUMN benchmarking_framework ENUM('RUNSOLVER', 'BENCHEXEC') NOT NULL DEFAULT 'RUNSOLVER';
