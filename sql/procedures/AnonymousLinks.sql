
DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement
 
DROP PROCEDURE IF EXISTS AddAnonymousLink;
CREATE PROCEDURE AddAnonymousLink(IN _uniqueId VARCHAR(36), IN _primitiveType ENUM('solver', 'bench', 'job'), IN _primitiveId INT, 
		IN _primitivesToAnonymize ENUM('all', 'allButBench', 'none'))
	BEGIN	
		INSERT INTO anonymous_links ( unique_id, primitive_type, primitive_id, primitives_to_anonymize, date_created ) 
			VALUES ( _uniqueId, _primitiveType, _primitiveId, _primitivesToAnonymize, CURDATE() );
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
CREATE PROCEDURE GetPrimitivesToAnonymize( IN _uniqueId VARCHAR(36) )
	BEGIN
		SELECT primitives_to_anonymize FROM anonymous_links WHERE _uniqueId = unique_id;
	END //

DELIMITER ;
