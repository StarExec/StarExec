USE starexec;

-- For Status Updates on a Benchmark upload
-- Author: Benton McCune
CREATE TABLE benchmark_uploads (
	id INT NOT NULL AUTO_INCREMENT, 
    space_id INT REFERENCES spaces(id) ON DELETE CASCADE,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    upload_time TIMESTAMP NOT NULL,
    file_upload_complete BOOLEAN DEFAULT 0,
    file_extraction_complete BOOLEAN DEFAULT 0,
    processing_begun BOOLEAN DEFAULT 0,
    everything_complete BOOLEAN DEFAULT 0,
    total_spaces INT DEFAULT 0,
    total_benchmarks INT DEFAULT 0,
    validated_benchmarks INT DEFAULT 0,
    completed_benchmarks INT DEFAULT 0,
    completed_spaces INT DEFAULT 0,
    error_message VARCHAR(512) DEFAULT "no error",
	PRIMARY KEY (id)
);