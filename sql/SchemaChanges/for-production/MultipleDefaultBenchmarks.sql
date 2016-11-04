USE starexec;

CREATE TABLE default_bench_assoc(
	community_id INT NOT NULL,
	bench_id INT NOT NULL,
	PRIMARY KEY(community_id, bench_id),
	CONSTRAINT default_bench_community_id FOREIGN KEY (community_id) REFERENCES spaces(id) ON DELETE CASCADE,
	CONSTRAINT default_bench_id FOREIGN KEY (bench_id) REFERENCES benchmarks(id) ON DELETE CASCADE
);

INSERT INTO default_bench_assoc (community_id, bench_id)
SELECT prim_id, default_benchmark
FROM default_settings
WHERE prim_id IS NOT NULL 
AND default_benchmark IS NOT NULL
AND setting_type=1;
