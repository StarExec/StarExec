package org.starexec.data.security;

import org.starexec.data.database.Queues;
import org.starexec.data.to.Queue;
import org.starexec.data.to.WorkerNode;

import java.util.List;

public class QueueSecurity {

	public static ValidatorStatusCode canUserClearErrorStates(int userId) {
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return new ValidatorStatusCode(false, "Only administrators can perform this action");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether the given user is allowed to make a new queue
	 *
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status from ValidatorStatusCodes if not
	 */

	public static ValidatorStatusCode canUserMakeQueue(int userId, String queueName) {
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to perform this operation");
		}

		// Make sure that the queue has a unique name
		if (Queues.notUniquePrimitiveName(queueName)) {
			return new ValidatorStatusCode(false, "The requested queue name is already in use. Please select another" +
					".");
		}

		return new ValidatorStatusCode(true);
	}

	/**
	 * Ensures a user has the appropriate permissions to edit an existing queue 
	 * and verifies that the timeOuts are allowed values
	 *
	 * @param userId The user making the request
	 * @param clockTimeout The new clock timeout to be given to the queue
	 * @param cpuTimeout The new cpu timeout to be given to the queue
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a different number otherwise
	 */

	public static ValidatorStatusCode canUserEditQueue(int userId, int clockTimeout, int cpuTimeout) {
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to perform this operation");
		}

		if (clockTimeout <= 0 || cpuTimeout <= 0) {
			return new ValidatorStatusCode(false, "All timeouts need to be greater than 0");
		}

		return new ValidatorStatusCode(true);
	}

	public static ValidatorStatusCode canUserEditQueue(int userId, int queueId) {
		if (Queues.get(queueId) == null) {
			return new ValidatorStatusCode(false, "The given queue could not be found");
		}
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to update the given queue");
		}
		return new ValidatorStatusCode(true);
	}

	public static ValidatorStatusCode canUserSetTestQueue(int userId, int queueId) {
		ValidatorStatusCode status = canUserEditQueue(userId, queueId);
		if (!status.isSuccess()) {
			return status;
		}
		List<WorkerNode> nodes = Queues.getNodes(queueId);
		if (nodes == null || nodes.isEmpty()) {
			return new ValidatorStatusCode(false, "The test queue should have some nodes");
		}
		return new ValidatorStatusCode(true);
	}

	public static ValidatorStatusCode canGetJsonQueue(int queueId, int userId) {

		List<Queue> queues = Queues.getUserQueues(userId);
		for (Queue q : queues) {
			if (q.getId() == queueId) {
				return new ValidatorStatusCode(true);
			}
		}

		return new ValidatorStatusCode(false, "The given queue does not exist or you do not have permission to see " +
				"it");
	}
}
