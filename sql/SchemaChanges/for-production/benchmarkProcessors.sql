USE starexec;

ALTER TABLE benchmarks
DROP FOREIGN KEY benchmarks_ibfk_2, ADD FOREIGN KEY (bench_type) REFERENCES processors(id) ON DELETE SET NULL;
