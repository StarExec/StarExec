USE starexec;

ALTER TABLE default_settings ADD benchmarking_framework ENUM('RUNSOLVER', 'BENCHEXEC') NOT NULL DEFAULT 'RUNSOLVER';
