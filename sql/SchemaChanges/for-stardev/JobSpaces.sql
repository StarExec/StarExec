USE starexec;

-- Saves all job space information
-- Author: Eric Burns
CREATE TABLE job_spaces (
	id INT NOT NULL AUTO_INCREMENT,
	name VARCHAR(128),
	PRIMARY KEY (id)
);


-- Saves associations between spaces relevent to a particular job
-- Author: Eric Burns
CREATE TABLE job_space_assoc (
	parent INT NOT NULL,
	child INT NOT NULL,
	FOREIGN KEY (parent) REFERENCES job_spaces(id) ON DELETE CASCADE,
	FOREIGN KEY (child) REFERENCES job_spaces(id) ON DELETE CASCADE
);

ALTER TABLE job_pairs 
ADD job_space_id INT NOT NULL;