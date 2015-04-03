-- Description: This file contains all website-related stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement



-- Adds a website that is associated with a user
-- Author: Skylar Stark	
DROP PROCEDURE IF EXISTS AddUserWebsite;
CREATE PROCEDURE AddUserWebsite(IN _userId INT, IN _url TEXT, IN _name VARCHAR(64))
	BEGIN
		INSERT INTO website(user_id, url, name)
		VALUES(_userId, _url, _name);
	END //
	
-- Adds a website that is associated with a solver
-- Author: Tyler Jensen	
DROP PROCEDURE IF EXISTS AddSolverWebsite;
CREATE PROCEDURE AddSolverWebsite(IN _solverId INT, IN _url TEXT, IN _name VARCHAR(64))
	BEGIN
		INSERT INTO website(solver_id, url, name)
		VALUES(_solverId, _url, _name);
	END //
	
-- Adds a website that is associated with a space (community)
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddSpaceWebsite;
CREATE PROCEDURE AddSpaceWebsite(IN _spaceId INT, IN _url TEXT, IN _name VARCHAR(64))
	BEGIN
		INSERT INTO website(space_id, url, name)
		VALUES(_spaceId, _url, _name);
	END //

-- Deletes the website with the given website id
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS DeleteWebsite;
CREATE PROCEDURE DeleteWebsite(IN _id INT)
	BEGIN
		DELETE FROM website
		WHERE id = _id;
	END // 

	
-- Returns all websites associated with the user with the given user id
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS GetWebsitesByUserId;
CREATE PROCEDURE GetWebsitesByUserId(IN _userid INT)
	BEGIN
		SELECT *
		FROM website
		WHERE website.user_id = _userid
		ORDER BY name;
	END //

-- Gets all websites that are associated with the solver with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetWebsitesBySolverId;
CREATE PROCEDURE GetWebsitesBySolverId(IN _id INT)
	BEGIN
		SELECT *
		FROM website
		WHERE website.solver_id = _id
		ORDER BY name;
	END //

-- Gets all websites that are associated with the space with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetWebsitesBySpaceId;
CREATE PROCEDURE GetWebsitesBySpaceId(IN _id INT)
	BEGIN
		SELECT *
		FROM website
		WHERE website.space_id = _id
		ORDER BY name;
	END //	

DROP PROCEDURE IF EXISTS GetWebsiteById;
CREATE PROCEDURE GetWebsiteById(IN _id INT)
BEGIN
	SELECT * FROM website WHERE id = _id;
END //

	

DELIMITER ; -- This should always be at the end of this file