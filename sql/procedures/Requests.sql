-- Description: This file contains all stored procedures used for requesting membership in a community, registering, and the resetting of passwords
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement



-- Adds an activation code for a specific user
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS AddCode;
CREATE PROCEDURE AddCode(IN _id INT, IN _code VARCHAR(36))
	BEGIN
		INSERT INTO verify(user_id, code, created)
		VALUES (_id, _code, SYSDATE());
	END //
	
-- Adds a request to join a community, provided the user isn't already a part of that community
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS AddCommunityRequest;
CREATE PROCEDURE AddCommunityRequest(IN _id INT, IN _community INT, IN _code VARCHAR(36), IN _message VARCHAR(300))
	BEGIN
		IF NOT EXISTS(SELECT * FROM user_assoc WHERE user_id = _id AND space_id = _community) THEN
			INSERT INTO community_requests(user_id, community, code, message, created)
			VALUES (_id, _community, _code, _message, SYSDATE());
		END IF;
	END //
	
-- Adds a request to reserve a queue
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS AddQueueRequest;
CREATE PROCEDURE AddQueueRequest(IN _userId INT, IN _spaceId INT, IN _queueName VARCHAR(64), IN _reserveDate DATE, IN _code VARCHAR(36), IN _message VARCHAR(300), IN _nodeCount INT)
	BEGIN
		INSERT INTO queue_request(user_id, space_id, queue_name, reserve_date, node_count, message, code, created)
		VALUES (_userId, _spaceId, _queueName, _reserveDate, _nodeCount, _message, _code, SYSDATE());
	END //
	
DROP PROCEDURE IF EXISTS UpdateQueueRequest;
CREATE PROCEDURE UpdateQueueRequest(IN _userId INT, IN _spaceId INT, IN _queueName VARCHAR(64), IN _reserveDate DATE, IN _code VARCHAR(36), IN _message VARCHAR(300), IN _nodeCount INT, IN _created TIMESTAMP)
	BEGIN
		INSERT INTO queue_request(user_id, space_id, queue_name, reserve_date, node_count, message, code, created)
		VALUES (_userId, _spaceId, _queueName, _reserveDate, _nodeCount, _message, _code, _created);
	END //

	
-- Adds a user to USER_ASSOC, deletes their entry in INVITES, and makes their
-- role 'user' if not so already
-- Author: Todd Elvers & Skylar Stark
DROP PROCEDURE IF EXISTS ApproveCommunityRequest;
CREATE PROCEDURE ApproveCommunityRequest(IN _id INT, IN _community INT)
	BEGIN
		DECLARE _newPermId INT;
		DECLARE _pid INT;	
		
		IF EXISTS(SELECT * FROM community_requests WHERE user_id = _id AND community = _community) THEN
			DELETE FROM community_requests 
			WHERE user_id = _id and community = _community;			
			
			-- Copy the default permission for the community 					
			SELECT default_permission FROM spaces WHERE id=_community INTO _pid;
			CALL CopyPermissions(_pid, _newPermId);
			
			INSERT INTO user_assoc(user_id, space_id, permission)
			VALUES(_id, _community, _newPermId);
			
			-- make the user a 'user' if they are currently 'unauthorized'
			IF EXISTS(SELECT email FROM user_roles WHERE email = (SELECT email FROM users WHERE users.id = _id) AND role = 'unauthorized') THEN
				UPDATE user_roles
				SET role = 'user'
				WHERE user_roles.email IN (SELECT email FROM users WHERE users.id = _id);
			END IF;
		END IF;
	END //
	
-- Adds a new entry to pass_reset_request for a given user (also deletes previous
-- entries for the same user)
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS AddPassResetRequest;
CREATE PROCEDURE AddPassResetRequest(IN _id INT, IN _code VARCHAR(36))
	BEGIN
		IF EXISTS(SELECT * FROM pass_reset_request WHERE user_id = _id) THEN
			DELETE FROM pass_reset_request
			WHERE user_id = _id;
		END IF;
		INSERT INTO pass_reset_request(user_id, code, created)
		VALUES(_id, _code, SYSDATE());
	END //
	
-- Deletes a user's entry in INVITES, and if the user is unregistered
-- (i.e. has a role of 'unauthorized') then they are completely
-- deleted from the system
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS DeclineCommunityRequest;
CREATE PROCEDURE DeclineCommunityRequest(IN _id INT, IN _community INT)
	BEGIN
		DELETE FROM community_requests 
		WHERE user_id = _id and community = _community;

		DELETE FROM users 
		WHERE users.id = _id
		AND users.email IN (SELECT email FROM user_roles WHERE role = 'unauthorized');
	END //
	
-- Returns the community request associated with given user id
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetCommunityRequestById;
CREATE PROCEDURE GetCommunityRequestById(IN _id INT)
	BEGIN
		SELECT *
		FROM community_requests
		WHERE user_id = _id;
	END //
	
-- Returns the community request associated with the given activation code
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetCommunityRequestByCode;
CREATE PROCEDURE GetCommunityRequestByCode(IN _code VARCHAR(36))
	BEGIN
		SELECT *
		FROM community_requests
		WHERE code = _code;
	END //
	
	
-- Returns the queue request associated with the given activation code
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetQueueRequestByCode;
CREATE PROCEDURE GetQueueRequestByCode(IN _code VARCHAR(36))
	BEGIN
		SELECT user_id, space_id, queue_name, MAX(node_count), MIN(reserve_date), MAX(reserve_date), message, code, created
		FROM queue_request
		WHERE code = _code
		GROUP BY user_id, space_id, queue_name;
	END //

-- Looks for an activation code, and if successful, removes it from VERIFY,
-- then adds an entry to USER_ROLES
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS RedeemActivationCode;
CREATE PROCEDURE RedeemActivationCode(IN _code VARCHAR(36), OUT _id INT)
	BEGIN
		IF EXISTS(SELECT _code FROM verify WHERE code = _code) THEN
			SELECT user_id INTO _id 
			FROM verify
			WHERE code = _code;
			
			DELETE FROM verify
			WHERE code = _code;
		END IF;
	END // 

-- Redeems a given password reset code by deleting the corresponding entry
-- in pass_reset_request and returning the user_id of that deleted entry
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS RedeemPassResetRequestByCode;
CREATE PROCEDURE RedeemPassResetRequestByCode(IN _code VARCHAR(36), OUT _id INT)
	BEGIN
		SELECT user_id INTO _id
		FROM pass_reset_request
		WHERE code = _code;
		DELETE FROM pass_reset_request
		WHERE code = _code;
	END //
	
	
-- Approves the resrvation of a queue by deleting it from the queue_request table
-- and then inserting into comm_queue table
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS ApproveQueueReservation;
CREATE PROCEDURE ApproveQueueReservation(IN _code VARCHAR(36), IN _spaceId INT, IN _queueId INT)
	BEGIN
		DELETE FROM queue_request 
		WHERE code = _code;
				
		INSERT INTO comm_queue
		VALUES (_spaceId, _queueId);
	END //
	
-- Declines the resrvation of a queue by deleting it from the queue_request table
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS DeclineQueueReservation;
CREATE PROCEDURE DeclineQueueReservation(IN _code VARCHAR(36))
	BEGIN
		DELETE FROM queue_request 
		WHERE code = _code;
	END //
	
	
-- Gets the number of queue reservations waiting approval
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetQueueRequestCount;
CREATE PROCEDURE GetQueueRequestCount()
	BEGIN
		SELECT DISTINCT count(DISTINCT user_id, space_id, queue_name) AS requestCount
		FROM queue_request;
	END //
	
-- Gets the number of community requests waiting approval
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetCommunityRequestCount;
CREATE PROCEDURE GetCommunityRequestCount()
	BEGIN
		SELECT count(*) AS requestCount
		FROM community_requests;
	END //
	
-- Gets the number of queue reservations
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetQueueReservationCount;
CREATE PROCEDURE GetQueueReservationCount()
	BEGIN
		SELECT count(*) AS reservationCount
		FROM comm_queue;
	END //
	
-- Gets the number of historic queue reservations
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetHistoricReservationCount;
CREATE PROCEDURE GetHistoricReservationCount()
	BEGIN
		SELECT count(*) AS reservationCount
		FROM reservation_history;
	END //
	
-- Deletes a queue reservation by removing it from comm_queue table
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS CancelQueueReservation;
CREATE PROCEDURE CancelQueueReservation(IN _queueId INT)
	BEGIN
		DELETE FROM comm_queue
		WHERE queue_id = _queueId;
		
		DELETE FROM queue_reserved
		WHERE queue_id = _queueId;
	END //
	
-- Updates the queue name of a queue_request
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS UpdateQueueName;
CREATE PROCEDURE UpdateQueueName(IN _code VARCHAR(36), IN _newName VARCHAR(64))
	BEGIN
		UPDATE queue_request
		SET queue_name = _newName
		WHERE code = _code;
	END //
	
-- Updates the maximum node count for a queue_request
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS UpdateNodeCount;
CREATE PROCEDURE UpdateNodeCount(IN _code VARCHAR(36), IN _nodeCount INT)
	BEGIN
		UPDATE queue_request
		SET node_count = _nodeCount
		WHERE code = _code;
	END //
	
	
-- Updates the start date of a queue_request
-- This is called when the new start date is later than the old one
-- Hence we need to remove entries from the queue_request table
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS UpdateStartDateToLaterDate;
CREATE PROCEDURE UpdateStartDateToLaterDate(IN _code VARCHAR(36), IN _startDate DATE)
	BEGIN
		DELETE FROM queue_request
		WHERE reserve_date < _startDate AND code = _code;
	END //
	
-- Updates the start date of a queue_request
-- This is called when the new start date is earlier than the old one
-- Hence we need to add entries to the queue_request table
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS UpdateStartDateToEarlierDate;
CREATE PROCEDURE UpdateStartDateToEarlierDate(IN _userId INT, IN _spaceId INT, IN _queueName VARCHAR(64), IN _nodeCount INT, IN _reserveDate DATE, IN _message TEXT, IN _code VARCHAR(36), IN _created TIMESTAMP )
	BEGIN
		INSERT INTO queue_request
		VALUES (_userId, _spaceId, _queueName, _nodeCount, _reserveDate, _message, _code, _created);
	END //
	
-- Updates the end date of a queue_request
-- This is called when the new end date is later than the old one
-- Hence we need to add entries to queue_request table
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS UpdateEndDateToLaterDate;
CREATE PROCEDURE UpdateEndDateToLaterDate(IN _userId INT, IN _spaceId INT, IN _queueName VARCHAR(64), IN _nodeCount INT, IN _reserveDate DATE, IN _message TEXT, IN _code VARCHAR(36), IN _created TIMESTAMP )
	BEGIN
		INSERT INTO queue_request
		VALUES (_userId, _spaceId, _queueName, _nodeCount, _reserveDate, _message, _code, _created);
	END //
	
-- Updates the end date of a queue_request
-- This is called when the new end date is earlier than the old one
-- Hence we need to remove entries from the queue_request table
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS UpdateEndDateToEarlierDate;
CREATE PROCEDURE UpdateEndDateToEarlierDate(IN _code VARCHAR(36), IN _startDate DATE)
	BEGIN
		DELETE FROM queue_request
		WHERE reserve_date > _startDate AND code = _code;
	END //
	
-- Get All the queue reservations
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetAllQueueReservations;
CREATE PROCEDURE GetAllQueueReservations()
	BEGIN
		SELECT space_id, queue_id, MIN(node_count), MAX(node_count), MIN(reserve_date), MAX(reserve_date), message
		FROM queue_reserved
		GROUP BY space_id, queue_id;
	END //
	
-- Adds an entry into reservation_history table
-- Author: Wyatt Kaiser	
DROP PROCEDURE IF EXISTS AddReservationToHistory;
CREATE PROCEDURE AddReservationToHistory(IN _spaceId INT, IN _queueName VARCHAR(64), IN _nodeCount INT, IN _startDate DATE, IN _endDate DATE, IN message TEXT)
	BEGIN
		INSERT INTO reservation_history
		VALUES (_spaceId, _queueName, _nodeCount, _startDate, _endDate, message);
	END //
	
-- Returns the nodeCount for a particular queue request on a particular date
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetRequestNodeCountOnDate;
CREATE PROCEDURE GetRequestNodeCountOnDate(IN _queuename VARCHAR(64), IN _reserveDate DATE)
	BEGIN
		SELECT node_count AS count
		FROM queue_request
		WHERE queue_name = _queuename AND reserve_date = _reserveDate;
	END //

-- Returns the the space_ID associated with a given queue_id
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetQueueReservationSpaceId;
CREATE PROCEDURE GetQueueReservationSpaceId ( IN _queueId INT) 
	BEGIN
		SELECT DISTINCT space_id
		FROM queue_reserved
		WHERE queue_id = _queueId;
	END //
	
-- Returns the the space_id associated with a given queue_name
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetQueueRequestSpaceId;
CREATE PROCEDURE GetQueueRequestSpaceId ( IN _queueName VARCHAR(64)) 
	BEGIN
		SELECT DISTINCT space_id
		FROM queue_request
		WHERE queue_name = _queueName;
	END //
	
DROP PROCEDURE IF EXISTS GetQueueRequestForReservation;
CREATE PROCEDURE GetQueueRequestForReservation( IN _queueId INT)
	BEGIN
		SELECT space_id, queue_id, MAX(node_count), MIN(reserve_date), MAX(reserve_date), message
		FROM queue_reserved
		WHERE queue_id = _queueId;
	END //
	
-- Gets the earliest end date of all reserved queues
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetEarliestEndDate;
CREATE PROCEDURE GetEarliestEndDate(IN _date DATE)
	BEGIN
		SELECT min(endDate)
		FROM (
			SELECT MAX(reserve_date) AS endDate
			FROM queue_reserved
			WHERE reserve_date >= _date
			GROUP BY queue_id
		) AS allEndDates;
	END //
	
DROP PROCEDURE IF EXISTS RemoveReservedEntries;
CREATE PROCEDURE RemoveReservedEntries(IN _queueId INT)
	BEGIN
		DELETE FROM queue_reserved
		WHERE queue_id = _queueId;
	END //
	
-- Decreases the node count of a reservation by 1
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS DecreaseNodeCount;
CREATE PROCEDURE DecreaseNodeCount(IN _queueId INT)
	BEGIN
		UPDATE queue_reserved
		SET node_count = node_count - 1
		WHERE queue_id = _queueId
		AND node_count > 0;
	END //
	
	
DELIMITER ; -- This should always be at the end of this file