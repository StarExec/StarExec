-- commments on what this does

DROP PROCEDURE IF EXISTS UpdateTo7_8 //
CREATE PROCEDURE UpdateTo7_8()
BEGIN
  IF EXISTS (SELECT 1 FROM system_flags WHERE major_version=1 AND minor_version=7) THEN
    ALTER TABLE benchmark_uploads
      ADD resumable TINYINT NOT NULL 
      DEFAULT 0;
    ALTER TABLE benchmark_uploads
      ADD path VARCHAR(256)
      DEFAULT NULL;
    ALTER TABLE benchmark_uploads
      ADD type_id INT
      DEFAULT NULL;
    ALTER TABLE benchmark_uploads
      ADD downloadable TINYINT
      DEFAULT NULL;
    ALTER TABLE benchmark_uploads
      ADD has_dependencies TINYINT
      DEFAULT NULL;
    ALTER TABLE benchmark_uploads
      ADD linked TINYINT
      DEFAULT NULL;
    ALTER TABLE benchmark_uploads
      ADD upload_method VARCHAR(256)
      DEFAULT NULL;
    ALTER TABLE benchmark_uploads
      ADD permission_id INT
      DEFAULT NULL;
  END IF;
END //

CALL UpdateTo7_8() //
DROP PROCEDURE IF EXISTS UpdateTo7_8 //