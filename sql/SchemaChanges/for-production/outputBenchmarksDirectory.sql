USE starexec;

ALTER TABLE jobs ADD COLUMN output_benchmarks_directory_path TEXT DEFAULT NULL;
