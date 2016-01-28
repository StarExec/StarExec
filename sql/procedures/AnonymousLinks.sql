
DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

DROP PROCEDURE IF EXISTS AddAnonymousLink;
CREATE PROCEDURE AddAnonymousLink( IN _uniqueId VARCHAR(36), IN _primitiveType VARCHAR(36), IN _primitiveId INT, IN _hidePrimitiveName BOOLEAN )
	BEGIN	
		INSERT INTO anonymous_links ( unique_id, primitive_type, primitive_id, hide_primitive_name ) 
			VALUES ( _uniqueId, _primitiveType, _primitiveId, _hidePrimitiveName );
	END //	

DROP PROCEDURE IF EXISTS GetAnonymousLink;
CREATE PROCEDURE GetAnonymousLink( IN _primitiveType VARCHAR(36), IN _primitiveId INT, IN _hidePrimitiveName BOOLEAN )
	BEGIN
		SELECT unique_id FROM anonymous_links 
			WHERE primitive_type = _primitiveType AND primitive_id = _primitiveId AND hide_primitive_name = _hidePrimitiveName;
	END //

DROP PROCEDURE IF EXISTS GetIdOfPrimitiveAssociatedWithLink;
CREATE PROCEDURE GetIdOfPrimitiveAssociatedWithLink( IN _uniqueId VARCHAR(36) )	
	BEGIN
		SELECT primitive_id FROM anonymous_links WHERE _uniqueId = unique_id;
	END // 

DROP PROCEDURE IF EXISTS IsPrimitiveNameHidden;
CREATE PROCEDURE IsPrimitiveNameHidden( IN _uniqueId VARCHAR(36) )
	BEGIN
		SELECT hide_primitive_name FROM anonymous_links WHERE _uniqueId = unique_id;
	END //

DELIMITER ;
