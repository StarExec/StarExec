USE starexec;


DROP TABLE IF EXISTS job_space_closure;
-- The set of all associations between each job space and it's descendants
CREATE TABLE job_space_closure (
	ancestor INT NOT NULL,
	descendant INT NOT NULL,
	last_used TIMESTAMP NOT NULL,
	UNIQUE KEY (ancestor, descendant),
	CONSTRAINT job_space_ancestor FOREIGN KEY (ancestor) REFERENCES job_spaces(id) ON DELETE CASCADE,
	CONSTRAINT job_space_descendant FOREIGN KEY (descendant) REFERENCES job_spaces(id) ON DELETE CASCADE
);