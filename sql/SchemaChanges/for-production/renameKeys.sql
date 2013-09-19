USE starexec;

drop table benchmark_uploads;

CREATE TABLE benchmark_uploads (
	id INT NOT NULL AUTO_INCREMENT, 
    space_id INT NOT NULL,
    user_id INT NOT NULL,
    upload_time TIMESTAMP NOT NULL,
    file_upload_complete BOOLEAN DEFAULT 0,
    file_extraction_complete BOOLEAN DEFAULT 0,
    processing_begun BOOLEAN DEFAULT 0,
    everything_complete BOOLEAN DEFAULT 0,
    total_spaces INT DEFAULT 0,
    total_benchmarks INT DEFAULT 0,
    validated_benchmarks INT DEFAULT 0,
    failed_benchmarks INT DEFAULT 0,
    completed_benchmarks INT DEFAULT 0,
    completed_spaces INT DEFAULT 0,
    error_message VARCHAR(512) DEFAULT "no error",
	PRIMARY KEY (id),
	CONSTRAINT benchmark_uploads_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	CONSTRAINT benchmark_uploads_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
