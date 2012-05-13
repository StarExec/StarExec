-- Description: This file contains all comment-related stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement


-- Adds a comment that is associated with a benchmark
-- Author: Vivek Sardeshmukh	
DROP PROCEDURE IF EXISTS AddBenchmarkComment;
CREATE PROCEDURE AddBenchmarkComment(IN _benchmarkId INT, IN _userId INT, IN _cmt TEXT)  
	BEGIN
		INSERT INTO comments(benchmark_id, user_id, cmt, cmt_date)
		VALUES(_benchmarkId, _userId, _cmt, SYSDATE());
	END //
	
-- Adds a comment that is associated with a solver
-- Author: Vivek Sardeshmukh	
DROP PROCEDURE IF EXISTS AddSolverComment;
CREATE PROCEDURE AddSolverComment(IN _solverId INT, IN _userId INT, IN _cmt TEXT)
	BEGIN
		INSERT INTO comments(solver_id, user_id, cmt, cmt_date)
		VALUES(_solverId, _userId, _cmt, SYSDATE());
	END //

-- Adds a comment that is associated with a space
-- Author: Vivek Sardeshmukh	
DROP PROCEDURE IF EXISTS AddSpaceComment;
CREATE PROCEDURE AddSpaceComment(IN _spaceId INT, IN _userId INT, IN _cmt TEXT)
	BEGIN
		INSERT INTO comments(space_id, user_id, cmt, cmt_date)
		VALUES(_spaceId, _userId, _cmt, SYSDATE());
	END //

	
-- Returns all comments associated with the benchmark with the given id
-- Author: Vivek Sardeshmukh
DROP PROCEDURE IF EXISTS GetCommentsByBenchmarkId;
CREATE PROCEDURE GetCommentsByBenchmarkId(IN _benchmarkId INT)
	BEGIN
		SELECT comments.id, user_id, first_name, last_name, cmt_date, cmt
		FROM comments, users
		WHERE comments.benchmark_id = _benchmarkId and comments.user_id = users.id
		ORDER BY cmt_date;
	END //

-- Gets all comments that are associated with the solver with the given id
-- Author: Vivek Sardeshmukh
DROP PROCEDURE IF EXISTS GetCommentsBySolverId;
CREATE PROCEDURE GetCommentsBySolverId(IN _solverId INT)
	BEGIN
		SELECT comments.id, user_id, first_name, last_name, cmt_date, cmt
		FROM comments, users
		WHERE comments.solver_id = _solverId and comments.user_id = users.id
		ORDER BY cmt_date;
	END //
	
-- Gets all comments that are associated with the solver with the given id
-- Author: Vivek Sardeshmukh
DROP PROCEDURE IF EXISTS GetCommentsBySpaceId;
CREATE PROCEDURE GetCommentsBySpaceId(IN _spaceId INT)
	BEGIN
		SELECT comments.id, user_id, first_name, last_name, cmt_date, cmt
		FROM comments, users
		WHERE comments.space_id = _spaceId and comments.user_id = users.id
		ORDER BY cmt_date;
	END //

-- Deletes a comment specified by a comment id
-- Author: Vivek Sardeshmukh
DROP PROCEDURE IF EXISTS DeleteComment;
CREATE PROCEDURE DeleteComment(IN _id INT)
	BEGIN
		DELETE FROM comments
		WHERE id=_id;
	END // 
	


DELIMITER ; -- This should always be at the end of this file