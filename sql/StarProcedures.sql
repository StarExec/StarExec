-- Description: This file contains all stored procedures for the starexec database --

USE starexec;

DROP PROCEDURE IF EXISTS GetPasswordById;
DROP PROCEDURE IF EXISTS GetUserByEmail;
DROP PROCEDURE IF EXISTS GetUserById;
DROP PROCEDURE IF EXISTS GetWebsitesById;
DROP PROCEDURE IF EXISTS UpdateEmail;
DROP PROCEDURE IF EXISTS UpdateFirstName;
DROP PROCEDURE IF EXISTS UpdateLastName;
DROP PROCEDURE IF EXISTS UpdateInstitution;
DROP PROCEDURE IF EXISTS UpdatePassword;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

CREATE PROCEDURE GetPasswordById(IN _id BIGINT)
	BEGIN
		SELECT password
		FROM users
		WHERE users.id = _id;
	END // 

CREATE PROCEDURE GetUserById(IN _id BIGINT)
	BEGIN
		SELECT * 
		FROM users NATURAL JOIN user_roles
		WHERE users.id = _id;
	END //

CREATE PROCEDURE GetUserByEmail(IN _email VARCHAR(64))
	BEGIN
		SELECT * 
		FROM users NATURAL JOIN user_roles
		WHERE users.email = _email;
	END //
	
CREATE PROCEDURE GetWebsitesById(IN _id BIGINT)
	BEGIN
		SELECT name, url
		FROM website
		WHERE website.user_id = _id;
	END //

CREATE PROCEDURE UpdateEmail(IN _id BIGINT, IN _email VARCHAR(64))
	BEGIN
		UPDATE users
		SET email = _email
		WHERE users.id = _id;
	END //
	
CREATE PROCEDURE UpdateFirstName(IN _id BIGINT, IN _firstname VARCHAR(32))
	BEGIN
		UPDATE users
		SET first_name = _firstname
		WHERE users.id = _id;
	END //

CREATE PROCEDURE UpdateLastName(IN _id BIGINT, IN _lastname VARCHAR(32))
	BEGIN
		UPDATE users
		SET last_name = _lastname
		WHERE users.id = _id;
	END //

CREATE PROCEDURE UpdateInstitution(IN _id BIGINT, IN _institution VARCHAR(64))
	BEGIN
		UPDATE users
		SET institution = _institution
		WHERE users.id = _id;
	END //

CREATE PROCEDURE UpdatePassword(IN _id BIGINT, IN _password VARCHAR(128))
	BEGIN
		UPDATE users
		SET password = _password
		WHERE users.id = _id;
	END //
	
DELIMITER ; -- This should always be at the end of this file