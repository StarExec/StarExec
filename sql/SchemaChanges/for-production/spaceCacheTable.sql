USE starexec;

-- This file adds the cache tables to the database
-- Author: Eric Burns

-- Associates space IDs with the cache of their downloads. cache_type refers to the type of the archive that is stored-- space,
-- solver, benchmark, job, etc
CREATE TABLE file_cache (
	id INT NOT NULL,
	path TEXT NOT NULL,
	cache_type INT NOT NULL,
	last_access TIMESTAMP NOT NULL,
	PRIMARY KEY (id,cache_type)
);