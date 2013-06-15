-- Description: This file contains all processor-related stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement


-- Adds a new processor with the given information
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddProcessor;
CREATE PROCEDURE AddProcessor(IN _name VARCHAR(64), IN _desc TEXT, IN _path TEXT, IN _comId INT, IN _type TINYINT, IN _diskSize BIGINT, OUT _id INT)
	BEGIN		
		INSERT INTO processors (name, description, path, community, processor_type, disk_size)
		VALUES (_name, _desc, _path, _comId, _type, _diskSize);
		
		SELECT LAST_INSERT_ID() INTO _id;
	END //
	
-- Removes the association between a processor and a given space,
-- and inserts the processor_path into _path, so the physical file(s) can
-- be removed from disk
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS DeleteProcessor;
CREATE PROCEDURE DeleteProcessor(IN _id INT, OUT _path TEXT)
	BEGIN
		SELECT path INTO _path FROM processors WHERE id = _id;
		DELETE FROM processors
		WHERE id = _id;
	END //
	
-- Gets all processors of a given type
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetAllProcessors;
CREATE PROCEDURE GetAllProcessors(IN _type TINYINT)
	BEGIN		
		SELECT *
		FROM processors
		WHERE processor_type=_type
		ORDER BY name;
	END //
	
-- Retrieves all processor belonging to a community
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetProcessorsByCommunity;
CREATE PROCEDURE GetProcessorsByCommunity(IN _id INT, IN _type TINYINT)
	BEGIN
		SELECT *
		FROM processors
		WHERE community=_id AND processor_type=_type
		ORDER BY name;
	END //
	
-- Gets the processor with the given ID
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetProcessorById;
CREATE PROCEDURE GetProcessorById(IN _id INT)
	BEGIN		
		SELECT *
		FROM processors
		WHERE id=_id;
	END //
	
-- Updates a processor's description
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateProcessorDescription;
CREATE PROCEDURE UpdateProcessorDescription(IN _id INT, IN _desc TEXT)
	BEGIN		
		UPDATE processors
		SET description=_desc
		WHERE id=_id;
	END //
	
-- Updates a processor's name
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateProcessorName;
CREATE PROCEDURE UpdateProcessorName(IN _id INT, IN _name VARCHAR(64))
	BEGIN		
		UPDATE processors
		SET name=_name
		WHERE id=_id;
	END //
	
-- Updates a processor's processor path
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateProcessorPath;
CREATE PROCEDURE UpdateProcessorPath(IN _id INT, IN _path TEXT, IN _diskSize BIGINT)
	BEGIN		
		UPDATE processors
		SET path=_path,
			disk_size=_diskSize
		WHERE id=_id;
	END //
	
	
DELIMITER ; -- This should always be at the end of this file