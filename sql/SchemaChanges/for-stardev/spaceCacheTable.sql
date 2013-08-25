USE starexec;

-- This file adds the space cache table to the database
-- Author: Eric Burns

-- Associates space IDs with the cache of their downloads, with multiple possibilities(maybe do solvers, benchmarks, solvers+benchmarks?)
CREATE TABLE space_cache (
	space_id INT NOT NULL,
	path TEXT NOT NULL,
	UNIQUE KEY (space_id),
	CONSTRAINT space_cache_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE
);