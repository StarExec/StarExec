-- Creates a new UpdateStatus entry when user uploads a benchmark
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS CreateBenchmarkUploadStatus //
CREATE PROCEDURE CreateBenchmarkUploadStatus(IN _spaceId INT, IN _userId INT, OUT id INT)
	BEGIN
		INSERT INTO benchmark_uploads (space_id, user_id, upload_time,error_message) VALUES (_spaceId, _userId, NOW(),"no error");
		SELECT LAST_INSERT_ID() INTO id;
	END //

-- Creates a new upload status entry for a space XML upload
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS CreateSpaceXMLUploadStatus //
CREATE PROCEDURE CreateSpaceXMLUploadStatus(IN _userId INT, OUT id INT)
	BEGIN
		INSERT INTO space_xml_uploads (user_id, upload_time,error_message) VALUES (_userId, NOW(), "no error");
		SELECT LAST_INSERT_ID() INTO id;
	END //

-- Updates status when file upload is complete
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS XMLFileUploadComplete //
CREATE PROCEDURE XMLFileUploadComplete(IN _id INT)
	BEGIN
		UPDATE space_xml_uploads
		SET file_upload_complete = 1
		WHERE id = _id;
	END //

-- Updates status when file upload is complete
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS BenchmarkFileUploadComplete //
CREATE PROCEDURE BenchmarkFileUploadComplete(IN _id INT)
	BEGIN
		UPDATE benchmark_uploads
		SET file_upload_complete = 1
		WHERE id = _id;
	END //

-- Updates status when file extraction is complete
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS FileExtractComplete //
CREATE PROCEDURE FileExtractComplete(IN _id INT)
	BEGIN
		UPDATE benchmark_uploads
		SET file_extraction_complete = 1
		WHERE id = _id;
	END //

-- Updates status when java object is created and processing/entering of benchmarks in db has begun
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS ProcessingBegun //
CREATE PROCEDURE ProcessingBegun(IN _id INT)
	BEGIN
		UPDATE benchmark_uploads
		SET processing_begun = 1
		WHERE id = _id;
	END //

-- Updates status when the entire upload benchmark process has completed
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS XMLEverythingComplete //
CREATE PROCEDURE XMLEverythingComplete(IN _id INT)
	BEGIN
		UPDATE space_xml_uploads
		SET everything_complete = 1
		WHERE id = _id;
	END //


DROP PROCEDURE IF EXISTS BenchmarkEverythingComplete //
CREATE PROCEDURE BenchmarkEverythingComplete(IN _id INT)
	BEGIN
		UPDATE benchmark_uploads
		SET everything_complete = 1
		WHERE id = _id;
	END //
-- Updates status when a directory is encountered when traversing extracted file
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS IncrementTotalSpaces //
CREATE PROCEDURE IncrementTotalSpaces(IN _id INT, IN _num INT)
	BEGIN
		UPDATE benchmark_uploads
		SET total_spaces = total_spaces + _num
		WHERE id = _id;
	END //

-- Updates status when a file is encountered when traversing extracted file
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS IncrementTotalBenchmarks //
CREATE PROCEDURE IncrementTotalBenchmarks(IN _id INT, _num INT)
	BEGIN
		UPDATE benchmark_uploads
		SET total_benchmarks = total_benchmarks + _num
		WHERE id = _id;
	END //

-- Indicates a space is completely added to the db.
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS IncrementCompletedSpaces //
CREATE PROCEDURE IncrementCompletedSpaces(IN _id INT, IN _num INT)
	BEGIN
		UPDATE benchmark_uploads
		SET completed_spaces = completed_spaces + _num
		WHERE id = _id;
	END //

-- Updates status when a benchmark is completed and entered into the db
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS IncrementCompletedBenchmarks //
CREATE PROCEDURE IncrementCompletedBenchmarks(IN _id INT, IN _num INT)
	BEGIN
		UPDATE benchmark_uploads
		SET completed_benchmarks = completed_benchmarks + _num
		WHERE id = _id;
	END //

-- Updates status when a benchmark is validated
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS IncrementValidatedBenchmarks //
CREATE PROCEDURE IncrementValidatedBenchmarks(IN _id INT, IN _num INT)
	BEGIN
		UPDATE benchmark_uploads
		SET validated_benchmarks = validated_benchmarks + _num
		WHERE id = _id;
	END //

-- Updates status when a benchmark fails validation
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS IncrementFailedBenchmarks //
CREATE PROCEDURE IncrementFailedBenchmarks(IN _id INT,IN _num INT)
	BEGIN
		UPDATE benchmark_uploads
		SET failed_benchmarks = failed_benchmarks + _num
		WHERE id = _id;
	END //


DROP PROCEDURE IF EXISTS SetXMLErrorMessage //
CREATE PROCEDURE SetXMLErrorMessage(IN _id INT, IN _message TEXT)
	BEGIN
		UPDATE space_xml_uploads
		SET error_message = _message
		WHERE id = _id;
	END //

-- Updates status when an error occurs
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS SetBenchmarkErrorMessage //
CREATE PROCEDURE SetBenchmarkErrorMessage(IN _id INT, IN _message TEXT)
	BEGIN
		UPDATE benchmark_uploads
		SET error_message = _message
		WHERE id = _id;
	END //

-- Retrieves the upload status with the given id
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS GetBenchmarkUploadStatusById //
CREATE PROCEDURE GetBenchmarkUploadStatusById(IN _id INT)
	BEGIN
		SELECT *
		FROM benchmark_uploads
		WHERE id = _id;
	END //

DROP PROCEDURE IF EXISTS GetUploadStatusForInvalidBenchmarkId //
CREATE PROCEDURE GetUploadStatusForInvalidBenchmarkId(IN _id INT)
	BEGIN
		SELECT benchmark_uploads.*
		FROM benchmark_uploads JOIN unvalidated_benchmarks ON benchmark_uploads.id=status_id
		WHERE unvalidated_benchmarks.id = _id;
	END //

-- Updates status when  benchmark fails validation
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS AddUnvalidatedBenchmark //
CREATE PROCEDURE AddUnvalidatedBenchmark(IN _id INT, IN _name VARCHAR(256), IN _error TEXT)
	BEGIN
		INSERT INTO unvalidated_benchmarks (status_id, bench_name, error_message)
		VALUES (_id, _name, _error);
	END //

-- Gets direct count of unvalidated benchmarks if there are no more than maximum
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS UnvalidatedBenchmarkCount //
CREATE PROCEDURE UnvalidatedBenchmarkCount(IN _status_id INT)
	BEGIN
		select count(*) from unvalidated_benchmarks
		WHERE status_id = _status_id;
	END //

-- Gets unvalidated benchmark names
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS GetUnvalidatedBenchmarks //
CREATE PROCEDURE GetUnvalidatedBenchmarks(IN _status_id INT)
	BEGIN
		select bench_name, id from unvalidated_benchmarks
		WHERE status_id = _status_id;
	END //

DROP PROCEDURE IF EXISTS SetXMLTotalSpaces //
CREATE PROCEDURE SetXMLTotalSpaces(IN _id INT, IN _num INT)
	BEGIN
		UPDATE space_xml_uploads
		SET total_spaces = _num
		WHERE id = _id;
	END //

DROP PROCEDURE IF EXISTS SetXMLTotalSolvers //
CREATE PROCEDURE SetXMLTotalSolvers(IN _id INT, IN _num INT)
	BEGIN
		UPDATE space_xml_uploads
		SET total_solvers = _num
		WHERE id = _id;
	END //

DROP PROCEDURE IF EXISTS SetXMLTotalBenchmarks //
CREATE PROCEDURE SetXMLTotalBenchmarks(IN _id INT, IN _num INT)
	BEGIN
		UPDATE space_xml_uploads
		SET total_benchmarks = _num
		WHERE id = _id;
	END //

DROP PROCEDURE IF EXISTS SetXMLTotalUpdates //
CREATE PROCEDURE SetXMLTotalUpdates(IN _id INT, IN _num INT)
	BEGIN
		UPDATE space_xml_uploads
		SET total_updates = _num
		WHERE id = _id;
	END //


DROP PROCEDURE IF EXISTS IncrementXMLCompletedUpdates //
CREATE PROCEDURE IncrementXMLCompletedUpdates(IN _id INT, IN _num INT)
	BEGIN
		UPDATE space_xml_uploads
		SET completed_updates = completed_updates +  _num
		WHERE id = _id;
	END //

DROP PROCEDURE IF EXISTS IncrementXMLCompletedSolvers //
CREATE PROCEDURE IncrementXMLCompletedSolvers(IN _id INT, IN _num INT)
	BEGIN
		UPDATE space_xml_uploads
		SET completed_solvers = completed_solvers +  _num
		WHERE id = _id;
	END //

DROP PROCEDURE IF EXISTS IncrementXMLCompletedBenchmarks //
CREATE PROCEDURE IncrementXMLCompletedBenchmarks(IN _id INT, IN _num INT)
	BEGIN
		UPDATE space_xml_uploads
		SET completed_benchmarks = completed_benchmarks +  _num
		WHERE id = _id;
	END //

DROP PROCEDURE IF EXISTS IncrementXMLCompletedSpaces //
CREATE PROCEDURE IncrementXMLCompletedSpaces(IN _id INT, IN _num INT)
	BEGIN
		UPDATE space_xml_uploads
		SET completed_spaces = completed_spaces +  _num
		WHERE id = _id;
	END //

-- Gets the error message for a particular row in the unvalidated benchmarks table
DROP PROCEDURE IF EXISTS GetInvalidBenchmarkMessage //
CREATE PROCEDURE GetInvalidBenchmarkMessage(IN _id INT)
	BEGIN
		SELECT error_message
		FROM unvalidated_benchmarks
		WHERE id = _id;
	END //
-- Gets the total count of the Uploads that belong to a specific user
DROP PROCEDURE IF EXISTS GetUploadCountByUser //
CREATE PROCEDURE GetUploadCountByUser(IN _userId INT)
        BEGIN
                SELECT COUNT(*) AS uploadCount
                FROM benchmark_uploads
                WHERE user_id = _userId;
        END //

DROP PROCEDURE IF EXISTS GetUploadCountByUserWithQuery //
CREATE PROCEDURE GetUploadCountByUserWithQuery(IN _userId INT, IN _query TEXT)
        BEGIN
                SELECT  COUNT(*) AS uploadCount
                FROM    benchmark_uploads
                WHERE   user_id=_userId AND
                                (upload_time  LIKE   CONCAT('%', _query, '%'));
        END //
