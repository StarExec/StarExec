-- Description: This file contains all cluster stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement


-- Adds a worker node to the database and ignores duplicates
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AssociateQueue;
CREATE PROCEDURE AssociateQueue(IN _queueName VARCHAR(64), IN _nodeName VARCHAR(64))
	BEGIN
		INSERT IGNORE INTO queue_assoc
		VALUES(
			(SELECT id FROM queues WHERE name=_queueName), 
			(SELECT id FROM nodes WHERE name=_nodeName));
	END //
	
-- Adds a worker node to the database and ignores duplicates
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddNode;
CREATE PROCEDURE AddNode(IN _name VARCHAR(64))
	BEGIN
		INSERT IGNORE INTO nodes (name)
		VALUES (_name);
	END //

-- Clear all Queue Associations from the db
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS ClearQueueAssociations;
CREATE PROCEDURE ClearQueueAssociations()
	BEGIN
		TRUNCATE queue_assoc;
	END //
	
-- Adds a SGE queue to the database and ignores duplicates
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddQueue;
CREATE PROCEDURE AddQueue(IN _name VARCHAR(64))
	BEGIN
		INSERT IGNORE INTO queues (name)
		VALUES (_name);
	END //
	

	
-- Gets the id, name and status of all nodes in the cluster that are active
-- Author: Tyler Jensen
-- TODO: What is the order by used for here?
DROP PROCEDURE IF EXISTS GetNodesForQueue;
CREATE PROCEDURE GetNodesForQueue(IN _id INT)
	BEGIN		
		SELECT node.id, node.name, node.status
		FROM queue_assoc
			JOIN nodes AS node ON node.id=queue_assoc.node_id
		WHERE _id=queue_assoc.queue_id
		ORDER BY name;	
	END //

-- Gets the id, name and status of all queues in the cluster that are active
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetAllQueues;
CREATE PROCEDURE GetAllQueues()
	BEGIN		
		SELECT id, name, status
		FROM queues
		WHERE status="ACTIVE"
		ORDER BY name;	
	END //
	
-- Gets the id, name and status of all queues in the cluster that are active and the user can use
-- That is, non exclusive queues and exclusive queues associated with spaces that the user is the leader of
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS GetUserQueues;
CREATE PROCEDURE GetUserQueues(IN _userID INT)
	BEGIN		
		SELECT id, name, status
		FROM queues
		WHERE status="ACTIVE"
		AND 
			(id NOT IN (select queue_id from comm_queue))
			   OR
			(id IN (select queue_id from comm_queue WHERE (IsLeader(space_id,_userId) = 1)))
		ORDER BY name;	
	END //	
	
-- Gets worker node with the given ID
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetNodeDetails;
CREATE PROCEDURE GetNodeDetails(IN _id INT)
	BEGIN		
		SELECT *
		FROM nodes
		WHERE id=_id;
	END // 
	
-- Gets the queue with the given ID (excluding SGE attributes)
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetQueue;
CREATE PROCEDURE GetQueue(IN _id INT)
	BEGIN		
		SELECT id, name, status, slots_used, slots_reserved, slots_free, slots_total
		FROM queues
		WHERE id=_id;
	END // 
	
-- Gets the queue with the given ID (includes all SGE attributes)
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetQueueDetails;
CREATE PROCEDURE GetQueueDetails(IN _id INT)
	BEGIN		
		SELECT *
		FROM queues
		WHERE id=_id;
	END // 
	
-- Updates a node's attribute (assuming the column already exists)
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateNodeAttr;
CREATE PROCEDURE UpdateNodeAttr(IN _name VARCHAR(64), IN _fieldName VARCHAR(64), IN _fieldVal VARCHAR(64))
	BEGIN	
		SET @updateAttr = CONCAT('UPDATE nodes SET ', _fieldName, '="', _fieldVal,'" WHERE name="', _name, '"');
		PREPARE stmt FROM @updateAttr;
		EXECUTE stmt;		
	END // 
	
-- Updates a queues's attribute (assuming the column already exists)
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateQueueAttr;
CREATE PROCEDURE UpdateQueueAttr(IN _name VARCHAR(64), IN _fieldName VARCHAR(64), IN _fieldVal VARCHAR(64))
	BEGIN	
		SET @updateAttr = CONCAT('UPDATE queues SET ', _fieldName, '="', _fieldVal,'" WHERE name="', _name, '"');
		PREPARE stmt FROM @updateAttr;
		EXECUTE stmt;		
	END // 
	
-- Updates a queues's usage stats
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateQueueUseage;
CREATE PROCEDURE UpdateQueueUseage(IN _name VARCHAR(64), IN _total INTEGER, IN _free INTEGER, IN _used INTEGER, IN _reserved INTEGER)
	BEGIN	
		UPDATE queues
		SET slots_total=_total, slots_free=_free, slots_used=_used, slots_reserved=_reserved
		WHERE name=_name;
	END // 

-- Updates all queues status'
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateAllQueueStatus;
CREATE PROCEDURE UpdateAllQueueStatus(IN _status VARCHAR(32))
	BEGIN	
		UPDATE queues
		SET status=_status;
	END // 
	
-- Updates a specific queues status
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateQueueStatus;
CREATE PROCEDURE UpdateQueueStatus(IN _name VARCHAR(64), IN _status VARCHAR(32))
	BEGIN	
		UPDATE queues
		SET status=_status
		WHERE name=_name;
	END // 

-- Updates all nodes status'
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateAllNodeStatus;
CREATE PROCEDURE UpdateAllNodeStatus(IN _status VARCHAR(32))
	BEGIN	
		UPDATE nodes
		SET status=_status;
	END // 
	
-- Updates a specific node's status
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateNodeStatus;
CREATE PROCEDURE UpdateNodeStatus(IN _name VARCHAR(64), IN _status VARCHAR(32))
	BEGIN	
		UPDATE nodes
		SET status=_status
		WHERE name=_name;
	END // 





DELIMITER ; -- This should always be at the end of this file