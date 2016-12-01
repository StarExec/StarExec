
-- Description: This file contains all procedures related to the pairs_rerun table.

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

DROP PROCEDURE IF EXISTS HasPairBeenRerun;
CREATE PROCEDURE HasPairBeenRerun(IN _pairId INT)
	BEGIN
		SELECT *
		FROM pairs_rerun
		WHERE pair_id=_pairId;
	END //

DROP PROCEDURE IF EXISTS MarkPairAsRerun;
CREATE PROCEDURE MarkPairAsRerun(IN _pairId INT)
	BEGIN
		INSERT INTO pairs_rerun (pair_id) 
		VALUES (_pairId);
	END //

DELIMITER ; -- This should always be at the end of this file
