USE starexec;

CREATE TABLE job_attributes_temp (
	pair_id INT NOT NULL,
	attr_key VARCHAR(128) NOT NULL,
	attr_value VARCHAR(128) NOT NULL,
	job_id INT NOT NULL,
        PRIMARY KEY (pair_id, attr_key) ,
        KEY (job_id) ,
        FOREIGN KEY (pair_id) REFERENCES job_pairs(id) ON DELETE CASCADE
);

INSERT IGNORE INTO job_attributes_temp SELECT * FROM job_attributes;

ALTER TABLE job_attributes RENAME TO job_attributes_old; 

ALTER TABLE job_attributes_temp RENAME TO job_attributes; 
