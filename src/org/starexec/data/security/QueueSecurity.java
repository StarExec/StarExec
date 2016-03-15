package org.starexec.data.security;

import java.util.List;

import org.starexec.data.database.Queues;
import org.starexec.data.database.Users;
import org.starexec.data.to.Queue;
import org.starexec.data.to.WorkerNode;
import org.starexec.util.Validator;

public class QueueSecurity {
	
	public static ValidatorStatusCode canUserClearErrorStates(int userId) {
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return new ValidatorStatusCode(false, "Only administrators can perform this action");
		}
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user is allowed to make a new queue
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status from ValidatorStatusCodes if not
	 */
	
	public static ValidatorStatusCode canUserMakeQueue(int userId, String queueName) {
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)){
			return new ValidatorStatusCode(false, "You do not have permission to perform this operation");
		}
		
		// Make sure that the queue has a unique name
		if(Queues.notUniquePrimitiveName(queueName)) {
			return new ValidatorStatusCode(false, "The requested queue name is already in use. Please select another.");
		}
		
		return new ValidatorStatusCode(true);
	}

	
	/**
	 * Ensures a user has the appropriate permissions to edit an existing queue with the given
	 * new values
	 * @param userId The user making the request
	 * @param clockTimeout The new clock timeout to be given to the queue
	 * @param cpuTimeout The new cpu timeout to be given to the queue
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a different number otherwise
	 */
	
	public static ValidatorStatusCode canUserEditQueue(int userId, int clockTimeout, int cpuTimeout) {
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to perform this operation");
		}
		
		if (clockTimeout<=0 || cpuTimeout<=0) {
			return new ValidatorStatusCode(false, "All timeouts need to be greater than 0");
		}
		
		return new ValidatorStatusCode(true);
	}

	public static ValidatorStatusCode canUserSetTestQueue(int userId, int queueId) {
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to modify queues");
		}
		List<WorkerNode> nodes = Queues.getNodes(queueId);
		if (nodes==null || nodes.size()==0) {
			return new ValidatorStatusCode(false, "The test queue should have some nodes");
		}
		return new ValidatorStatusCode(true);
	}
	
	public static ValidatorStatusCode canGetJsonQueue(int queueId, int userId) {
		
		List<Queue> queues=Queues.getQueuesForUser(userId);
		for (Queue q : queues) {
			if (q.getId()==queueId) {
				return new ValidatorStatusCode(true);
			}
		}
		
		return new ValidatorStatusCode(false, "The given queue does not exist or you do not have permission to see it");
	}
	
	public static ValidatorStatusCode canUserSubmitToQueue(int userId, int queueId, int spaceId) {
		if (Queues.get(queueId)==null) {
			return new ValidatorStatusCode(false, "The given queue could not be found");
		}
		List<Queue> validQueues=Queues.getQueuesForUser(userId);
		validQueues.addAll(Queues.getQueuesForSpace(spaceId));
		for (Queue q : validQueues) {
			if (q.getId()==queueId) {
				return new ValidatorStatusCode(true);
			}
		}
		
		return new ValidatorStatusCode(false, "You do not have permission to utilize the selected queue");
	}
}
