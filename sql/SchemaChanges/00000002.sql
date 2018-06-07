-- Add `runscript_errors` table
-- This table keeps track of all runscript errors so that we can see if a
-- particular node starts reporting too many errors, and try to prevent it from
-- turning into a black hole for JobPairs

DROP PROCEDURE IF EXISTS UpdateTo1_2 //
CREATE PROCEDURE UpdateTo1_2()
BEGIN
	IF EXISTS (SELECT 1 FROM system_flags WHERE major_version=1 AND minor_version=1) THEN
		UPDATE system_flags SET minor_version=2;

		CREATE TABLE runscript_errors (
			id INT NOT NULL AUTO_INCREMENT,
			node_id INT NOT NULL,
			job_pair_id INT NOT NULL,
			time TIMESTAMP NOT NULL DEFAULT NOW(),
			PRIMARY KEY (id),
			CONSTRAINT runscript_errors_node_id FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE CASCADE,
			CONSTRAINT runscript_errors_job_pair_id FOREIGN KEY (job_pair_id) REFERENCES job_pairs(id)
		);
	END IF;
END //

CALL UpdateTo1_2() //
DROP PROCEDURE IF EXISTS UpdateTo1_2 //
