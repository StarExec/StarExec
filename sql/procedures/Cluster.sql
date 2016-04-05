-- Description: This file contains all cluster stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

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
	
-- Gets the id, name and status of all nodes in the cluster that are active
-- Author: Tyler Jensen
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
		SELECT id, name, status,global_access, cpuTimeout,clockTimeout
		FROM queues
		WHERE status="ACTIVE"
		ORDER BY name;	
	END //
	
-- Gets the id, name and status of all queues in the cluster
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetAllQueuesAdmin;
CREATE PROCEDURE GetAllQueuesAdmin()
	BEGIN		
		SELECT id, name, status,global_access, cpuTimeout, clockTimeout
		FROM queues
		ORDER BY id;	
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
		SELECT *
		FROM queues
		WHERE id=_id;
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
	
-- Returns all the nodes in the system that are active
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetAllNodes;
CREATE PROCEDURE GetAllNodes ()
	BEGIN
		SELECT *
		FROM nodes
		WHERE status = "ACTIVE";
	END //
	
-- Returns all the nodes in the system that are active and not associated with the queue already
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetNonAttachedNodes;
CREATE PROCEDURE GetNonAttachedNodes(IN _queueId INT)
	BEGIN
		SELECT DISTINCT nodes.id, queues.id, nodes.name, queues.name, nodes.status
		FROM nodes LEFT JOIN queue_assoc on nodes.id = queue_assoc.node_id 
		LEFT JOIN queues ON queues.id=queue_assoc.queue_id
		WHERE nodes.status = "ACTIVE" AND (queue_assoc.queue_id IS NULL OR queue_assoc.queue_id != _queueId);
	END //
	
-- Returns the jobs that are currently running on a specific queue
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetJobsRunningOnQueue;
CREATE PROCEDURE GetJobsRunningOnQueue(IN _queueId INT)
	BEGIN
		SELECT DISTINCT
			jobs.id, 
			jobs.name, 
			jobs.user_id,
			jobs.queue_id,
			jobs.created, 
			jobs.completed,
			jobs.description, 
			jobs.deleted,
			jobs.primary_space,
			GetJobStatus(jobs.id)		AS status,
			GetTotalPairs(jobs.id) 		AS totalPairs,
			GetCompletePairs(jobs.id) 	AS completePairs,
			GetPendingPairs(jobs.id) 	AS pendingPairs,
			GetErrorPairs(jobs.id) 		AS errorPairs
		
		FROM	jobs
		JOIN    job_pairs ON jobs.id = job_pairs.job_id
		WHERE 	job_pairs.status_code < 7 AND jobs.queue_id = _queueId;
	END //
	
-- Returns the Queue that a specific node is associated with
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetQueueForNode;
CREATE PROCEDURE GetQueueForNode(IN _nodeId INT)
	BEGIN
		SELECT queues.id, queues.name, queues.status
		FROM queues, queue_assoc
		WHERE queues.id = queue_assoc.queue_id AND queue_assoc.node_id = _nodeId;
	END //
	
-- Return the node id given its name
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetNodeIdByName;
CREATE PROCEDURE GetNodeIdByName(IN _nodeName VARCHAR(128))
	BEGIN
		SELECT id
		FROM nodes
		WHERE name = _nodeName;
	END //
	
-- Return the node name given its id
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetNodeNameById;
CREATE PROCEDURE GetNodeNameById(IN _nodeId INT)
	BEGIN
		SELECT name
		FROM nodes
		WHERE id = _nodeId;
	END //

-- deletes a node from the database
DROP PROCEDURE IF EXISTS DeleteNode;
CREATE PROCEDURE DeleteNode(IN _id INT)
	BEGIN
		DELETE FROM nodes WHERE id=_id;
	END //

DELIMITER ; -- This should always be at the end of this file