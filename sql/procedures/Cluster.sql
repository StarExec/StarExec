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
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS AssociateQueueById;
CREATE PROCEDURE AssociateQueueById(IN _queueId INT, IN _nodeId INT)
	BEGIN
		DELETE FROM queue_assoc
		WHERE node_id = _nodeId;
		
		INSERT IGNORE INTO queue_assoc
		VALUES(_queueId, _nodeId);

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
	
-- Gets the id, name and status of all queues in the cluster
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetAllQueuesAdmin;
CREATE PROCEDURE GetAllQueuesAdmin()
	BEGIN		
		SELECT id, name, status
		FROM queues
		ORDER BY id;	
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
	
-- Gets the id, name and status of all queues in the cluster that are active and the user can use and are unreserved
-- That is, non exclusive queues and exclusive queues associated with spaces that the user is the leader of
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS GetUnreservedQueues;
CREATE PROCEDURE GetUnreservedQueues(IN _userID INT)
	BEGIN		
		SELECT id, name, status
		FROM queues
		WHERE status="ACTIVE"
		AND 
			(id NOT IN (select queue_id from comm_queue))
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

-- Get the number of active nodes
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetActiveNodeCount;
CREATE PROCEDURE GetActiveNodeCount()
	BEGIN
		SELECT Count(*)
		AS nodeCount
		FROM nodes
		WHERE status = "ACTIVE";
	END //
	
-- Returns the node count for a particular date for a particular queue
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetNodeCountOnDate;
CREATE PROCEDURE GetNodeCountOnDate(_queueId INT, IN _reserveDate DATE)
	BEGIN
		SELECT node_count AS count
		FROM queue_reserved
		WHERE queue_id = _queueId AND reserve_date = _reserveDate;
	END //
	
-- Returns the latest date in the queue_reserved table 
-- Author: Wyatt Kaiser	
DROP PROCEDURE IF EXISTS GetLatestNodeDate;
CREATE PROCEDURE GetLatestNodeDate()
	BEGIN
		SELECT MAX(reserve_date)
		FROM queue_reserved;
	END //
	
DROP PROCEDURE IF EXISTS UpdateReservedNodeCount;
CREATE PROCEDURE UpdateReservedNodeCount(IN _spaceId INT, IN _queueId INT, IN _nodeCount INT, IN _date DATE)
	BEGIN
		INSERT INTO queue_reserved
		VALUES (_spaceId, _queueId, _nodeCount, _date)
		ON DUPLICATE KEY UPDATE
		node_count=_nodeCount;
	END //
	
-- Deletes all entries from the temporary temp_node_changes table
-- Author: Wyatt Kaiser	
DROP PROCEDURE IF EXISTS RefreshTempNodeChanges;
CREATE PROCEDURE RefreshTempNodeChanges()
	BEGIN
		DELETE FROM temp_node_changes;
	END //
	
-- Adds an entry to temporary temp_node_changes table
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS AddTempNodeChange;
CREATE PROCEDURE AddTempNodeChange(IN _spaceId INT, IN _queueName VARCHAR(64), IN _nodeCount INT, IN _reserveDate DATE)
	BEGIN
		INSERT INTO temp_node_changes
		VALUES (_spaceId, _queueName, _nodeCount, _reserveDate)
		ON DUPLICATE KEY UPDATE
		node_count=_nodeCount;
	END //
	
-- Returns the temp nodeCount for a particular queue on a particular date
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetTempNodeCountOnDate;
CREATE PROCEDURE GetTempNodeCountOnDate(IN _queuename VARCHAR(64), IN _reserveDate DATE)
	BEGIN
		SELECT node_count AS count
		FROM temp_node_changes
		WHERE queue_name = _queuename AND reserve_date = _reserveDate;
	END //
	
-- Returns the queueName, nodeCount, and reserveDate from the temp_node_changes table
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetTempChanges;
CREATE PROCEDURE GetTempChanges()
	BEGIN
		SELECT * 
		FROM temp_node_changes;
	END //
	
-- Returns the minimum node count for a queue reservation
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetMinNodeCount;
CREATE PROCEDURE GetMinNodeCount( IN _queueId INT )
	BEGIN
		SELECT MIN(node_count) AS count
		FROM queue_reserved
		WHERE queue_id = _queueId;
	END //
	
-- Returns the maximum node count for a queue reservation
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetMaxNodeCount;
CREATE PROCEDURE GetMaxNodeCount (IN _queueId INT)
	BEGIN
		SELECT MAX(node_count) AS count
		FROM queue_reserved
		WHERE queue_id = _queueId;
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
			jobs.created, 
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
	
DROP PROCEDURE IF EXISTS GetNextPageOfNodesAdmin;
CREATE PROCEDURE GetNextPageOfNodesAdmin(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _query TEXT)
	BEGIN
		-- If _query is empty, get next page of Users without filtering for _query
		IF (_query = '' OR _query = NULL) THEN
			IF _sortASC = TRUE THEN
				SELECT 	id,
						name,
						status
						
				FROM	nodes

				-- Order results depending on what column is being sorted on
				ORDER BY 
				(CASE _colSortedOn
					WHEN 0 THEN name
					WHEN 1 THEN status
				END) ASC
				
				-- Shrink the results to only those required for the next page of Users
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id,
						name,
						status
						
				FROM	nodes 
				ORDER BY 
				(CASE _colSortedOn
					WHEN 0 THEN name
					WHEN 1 THEN status
				END) DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		-- Otherwise, ensure the target Nodes contain _query
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	id,
						name,
						status
				
				FROM	nodes
							
				-- Exclude Users whose name and description don't contain the query string
				WHERE 	(name								LIKE	CONCAT('%', _query, '%')
				OR		status								LIKE 	CONCAT('%', _query, '%'))
								
				-- Order results depending on what column is being sorted on
				ORDER BY 
				(CASE _colSortedOn
					WHEN 0 THEN name
					WHEN 1 THEN status
				END) ASC
					 
				-- Shrink the results to only those required for the next page of Users
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id,
						name,
						status
						
				FROM	nodes
				WHERE
						(name								LIKE	CONCAT('%', _query, '%')
				OR		status								LIKE 	CONCAT('%', _query, '%'))
				ORDER BY 
				(CASE _colSortedOn
					WHEN 0 THEN name
					WHEN 1 THEN status
				END) DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //





DELIMITER ; -- This should always be at the end of this file