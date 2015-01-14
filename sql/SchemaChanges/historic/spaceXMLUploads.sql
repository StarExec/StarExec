USE starexec;

-- For Status Updates on a space XML upload
-- Author: Eric Burns
CREATE TABLE space_xml_uploads (
	id INT NOT NULL AUTO_INCREMENT, 
    user_id INT NOT NULL,
    upload_time TIMESTAMP NOT NULL,
    file_upload_complete BOOLEAN DEFAULT 0,
    everything_complete BOOLEAN DEFAULT 0,
    total_spaces INT DEFAULT 0,
    completed_spaces INT DEFAULT 0,
    total_benchmarks INT DEFAULT 0,
    completed_benchmarks INT DEFAULT 0,
    total_solvers INT DEFAULT 0,
    completed_solvers INT DEFAULT 0,
    total_updates INT DEFAULT 0,
    completed_updates INT DEFAULT 0,
    error_message TEXT,
	PRIMARY KEY (id),
	CONSTRAINT space_xml_uploads_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);