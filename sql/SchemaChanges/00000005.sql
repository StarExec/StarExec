-- Modify timestamp column so they are not updated automatically

DROP PROCEDURE IF EXISTS UpdateTo4_5 //
CREATE PROCEDURE UpdateTo4_5()
BEGIN
	IF EXISTS (SELECT 1 FROM system_flags WHERE major_version=1 AND minor_version=4) THEN
		UPDATE system_flags SET minor_version=5;
		ALTER TABLE users              CHANGE COLUMN created     created     TIMESTAMP DEFAULT NOW();
		ALTER TABLE spaces             CHANGE COLUMN created     created     TIMESTAMP DEFAULT NOW();
		ALTER TABLE benchmarks         CHANGE COLUMN uploaded    uploaded    TIMESTAMP DEFAULT NOW();
		ALTER TABLE solvers            CHANGE COLUMN uploaded    uploaded    TIMESTAMP DEFAULT NOW();
		ALTER TABLE solver_pipelines   CHANGE COLUMN uploaded    uploaded    TIMESTAMP DEFAULT NOW();
--		ALTER TABLE jobs               CHANGE COLUMN completed   completed   TIMESTAMP DEFAULT NOW();
		ALTER TABLE verify             CHANGE COLUMN created     created     TIMESTAMP DEFAULT NOW();
		ALTER TABLE community_requests CHANGE COLUMN created     created     TIMESTAMP DEFAULT NOW();
		ALTER TABLE pass_reset_request CHANGE COLUMN created     created     TIMESTAMP DEFAULT NOW();
		ALTER TABLE benchmark_uploads  CHANGE COLUMN upload_time upload_time TIMESTAMP DEFAULT NOW();
	END IF;
END //

CALL UpdateTo4_5() //
DROP PROCEDURE IF EXISTS UpdateTo4_5 //
