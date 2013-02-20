USE prod_starexec;

ALTER TABLE benchmark_uploads
ADD failed_benchmarks INT DEFAULT 0;

-- For benchmarks that fail validation
-- Author: Benton McCune
CREATE TABLE unvalidated_benchmarks (
	id INT NOT NULL AUTO_INCREMENT, 
    status_id INT REFERENCES benchmark_uploads(id) ON DELETE CASCADE,
    bench_name VARCHAR(256) NOT NULL,
	PRIMARY KEY (id)
);
