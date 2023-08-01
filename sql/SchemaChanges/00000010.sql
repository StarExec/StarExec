-- this change updates the job_stats table which serves as a cache
-- for queries about job stats from the front end. This changes a 
-- table originally created by Eric Burns, the documentation which 
-- I have included his docs as a reference below.

-- Stores a cache of stats for a particular job space. Incomplete pairs are not stored,
-- as we only store complete jobs, so incomplete=failed. Stats are hierarchical,
-- so stats at a particular job space include all pairs below that job space
-- Author: Eric Burns


-- Author: aguo2

DROP PROCEDURE IF EXISTS UpdateTo9_10 //
CREATE PROCEDURE UpdateTo9_10()
BEGIN
    DROP TABLE IF EXISTS job_stats;
    CREATE TABLE job_stats (
	job_space_id INT NOT NULL,
	config_id INT NOT NULL,
	complete INT NOT NULL,
	correct INT NOT NULL,
	incorrect INT NOT NULL,
	incomplete INT NOT NULL,
	conflicts INT NOT NULL,
	failed INT NOT NULL,
	wallclock DOUBLE,
	cpu DOUBLE,
	resource_out INT NOT NULL,
	stage_number INT NOT NULL DEFAULT 0, -- what stage is this? from 1...n, with 0 meaning the primary stage
    include_unknowns BOOLEAN NOT NULL DEFAULT FALSE,
	PRIMARY KEY (job_space_id,config_id,stage_number, include_unknowns),
	CONSTRAINT job_stats_job_space_id FOREIGN KEY (job_space_id) REFERENCES job_spaces(id) ON DELETE CASCADE,
	KEY (config_id)
);
END //

CALL UpdateTo9_10() //
DROP PROCEDURE IF EXISTS UpdateTo9_10 //

