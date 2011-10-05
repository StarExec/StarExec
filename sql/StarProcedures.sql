-- Description: This file contains all stored procedures for the starexec database --

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

CREATE PROCEDURE GetUserByEmail(IN _email VARCHAR(64))
	BEGIN
		SELECT * 
		FROM users NATURAL JOIN user_roles
		WHERE users.email = _email;
	END //
	
CREATE PROCEDURE GetUserById(IN _id BIGINT)
	BEGIN
		SELECT * 
		FROM users NATURAL JOIN user_roles
		WHERE users.id = _id;
	END //
	
DELIMITER ; -- This should always be at the end of this file