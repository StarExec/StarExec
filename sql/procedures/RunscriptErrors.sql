-- Description: This file contains all Runscript Error procedures

DROP PROCEDURE IF EXISTS RunscriptError //
CREATE PROCEDURE RunscriptError(IN node VARCHAR(32), IN jobPairId INT, IN stage INT)
	BEGIN
		SET @node_id := (
			SELECT id
			FROM nodes
			WHERE name=node
		);

		INSERT INTO runscript_errors (node_id, job_pair_id)
		VALUES (@node_id, jobPairId);

		CALL UpdatePairStatus(jobPairId, 11);
		CALL UpdateLaterStageStatuses(jobPairId, stage, 11);
		CALL SetRunStatsForLaterStagesToZero(jobPairId, stage);
	END //

DROP PROCEDURE IF EXISTS GetRunscriptErrorsCount //
CREATE PROCEDURE GetRunscriptErrorsCount(IN _begin TIMESTAMP, IN _end TIMESTAMP)
	BEGIN
		SELECT COUNT(*) as 'count'
		FROM runscript_errors
		WHERE time <= _begin
		  AND time >= _end;
	END //

DROP PROCEDURE IF EXISTS GetRunscriptErrors //
CREATE PROCEDURE GetRunscriptErrors(IN _begin TIMESTAMP, IN _end TIMESTAMP)
	BEGIN
		SELECT name AS node, job_pair_id, time
		FROM runscript_errors
		JOIN nodes on nodes.id=node_id
		WHERE time <= _begin
		  AND time >= _end;
	END //
