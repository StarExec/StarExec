USE starexec;

CREATE TABLE default_bench_assoc(
	setting_id INT NOT NULL,
	bench_id INT NOT NULL,
	PRIMARY KEY(setting_id, bench_id),
	CONSTRAINT default_setting_id FOREIGN KEY (setting_id) REFERENCES default_settings(id) ON DELETE CASCADE,
	CONSTRAINT default_bench_id FOREIGN KEY (bench_id) REFERENCES benchmarks(id) ON DELETE CASCADE
);

INSERT INTO default_bench_assoc (setting_id, bench_id)
SELECT id, default_benchmark
FROM default_settings
WHERE prim_id IS NOT NULL 
AND default_benchmark IS NOT NULL;
