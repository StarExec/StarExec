
DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

DROP PROCEDURE IF EXISTS AddAnonymousPrimitiveName;
CREATE PROCEDURE AddAnonymousPrimitiveName( 
		IN _anonymousName VARCHAR(36), 
		IN _primitiveId INT, 
		IN _primitiveType ENUM('solver', 'bench', 'job', 'config'),
		IN _jobId INT)
	BEGIN
		INSERT INTO anonymous_primitive_names ( anonymous_name, primitive_id, primitive_type, job_id)
			VALUES ( _anonymousName, _primitiveId, _primitiveType, _jobId);
	END //

DROP PROCEDURE IF EXISTS AddAnonymousLink;
CREATE PROCEDURE AddAnonymousLink(IN _uniqueId VARCHAR(36), IN _primitiveType ENUM('solver', 'bench', 'job'), IN _primitiveId INT, 
		IN _primitivesToAnonymize ENUM('all', 'allButBench', 'none'))
	BEGIN	
		INSERT INTO anonymous_links ( unique_id, primitive_type, primitive_id, primitives_to_anonymize, date_created ) 
			VALUES ( _uniqueId, _primitiveType, _primitiveId, _primitivesToAnonymize, CURDATE() );
	END //	

DROP PROCEDURE IF EXISTS GetAnonymousNamesForJob;
CREATE PROCEDURE GetAnonymousNamesForJob( IN _jobId INT )
	BEGIN
		SELECT * FROM anonymous_primitive_names WHERE job_id=_jobId;
	END // 

DROP PROCEDURE IF EXISTS GetAnonymousSolverNamesAndIds;
CREATE PROCEDURE GetAnonymousSolverNamesAndIds( IN _jobId INT )
	BEGIN
		SELECT anonymous_name, primitive_id FROM anonymous_primitive_names WHERE job_id=_jobId AND primitive_type="solver";
	END // 

DROP PROCEDURE IF EXISTS GetAnonymousLink;
CREATE PROCEDURE GetAnonymousLink( IN _primitiveType ENUM('solver', 'bench', 'job'), IN _primitiveId INT, 
		IN _primitivesToAnonymize ENUM('all', 'allButBench', 'none') )
	BEGIN
		SELECT unique_id FROM anonymous_links 
			WHERE primitive_type = _primitiveType AND primitive_id = _primitiveId AND primitives_to_anonymize = _primitivesToAnonymize;
	END //

DROP PROCEDURE IF EXISTS GetIdOfPrimitiveAssociatedWithLink;
CREATE PROCEDURE GetIdOfPrimitiveAssociatedWithLink( IN _uniqueId VARCHAR(36), IN _primitiveType ENUM('solver', 'bench', 'job') )	
	BEGIN
		SELECT primitive_id FROM anonymous_links WHERE unique_id = _uniqueId AND primitive_type = _primitiveType;
	END // 

DROP PROCEDURE IF EXISTS GetPrimitivesToAnonymize;
CREATE PROCEDURE GetPrimitivesToAnonymize( IN _uniqueId VARCHAR(36), IN _primitiveType ENUM('solver', 'bench', 'job'))
	BEGIN
		SELECT primitives_to_anonymize FROM anonymous_links WHERE _uniqueId = unique_id AND primitive_type = _primitiveType;
	END //

DROP PROCEDURE IF EXISTS DeleteOldLinks;
CREATE PROCEDURE DeleteOldLinks( IN _ageThresholdInDays INT )
	BEGIN
		/* Delete all the anonymous primitive names that correspond to anonymous links for jobs that are being deleted. */
		DELETE FROM anonymous_primitive_names WHERE job_id IN (SELECT primitive_id FROM anonymous_links 
				WHERE DATEDIFF( CURDATE(), date_created ) >= _ageThresholdInDays AND primitive_type="job");

		DELETE FROM anonymous_links WHERE DATEDIFF( CURDATE(), date_created ) >= _ageThresholdInDays; 
	END //

DROP PROCEDURE IF EXISTS DeleteAnonymousLink;
CREATE PROCEDURE DeleteAnonymousLink( IN _uniqueId VARCHAR(36) )
	BEGIN
		DELETE FROM anonymous_primitive_names WHERE anonymous_primitive_names.job_id IN 
				(SELECT anonymous_links.primitive_id FROM anonymous_links WHERE anonymous_links.unique_id=_uniqueId AND anonymous_links.primitive_type="job");
		DELETE FROM anonymous_links WHERE unique_id = _uniqueId;
	END //

DELIMITER ;
