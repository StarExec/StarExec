package org.starexec.test.security;

import org.junit.Assert;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Users;
import org.starexec.data.security.QueueSecurity;
import org.starexec.data.to.User;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.TestUtil;
import org.starexec.test.resources.ResourceLoader;

public class QueueSecurityTests extends TestSequence {

	User user1=null;
	User user2=null;
	User admin=null;
	
	@Test
	private void CancelRequestTest() {
		Assert.assertEquals(0,QueueSecurity.canUserCancelRequest(admin.getId()));
		Assert.assertNotEquals(0,QueueSecurity.canUserCancelRequest(user1.getId()));
		Assert.assertNotEquals(0,QueueSecurity.canUserCancelRequest(user2.getId()));
	}
	
	@Test
	private void PermanentQueueTest() {
		Assert.assertEquals(0,QueueSecurity.canUserMakeQueue(admin.getId()));
		Assert.assertNotEquals(0,QueueSecurity.canUserMakeQueue(user1.getId()));
		Assert.assertNotEquals(0,QueueSecurity.canUserMakeQueue(user2.getId()));
	}
	
	@Test
	private void RemoveQueueTest() {
		Assert.assertEquals(0,QueueSecurity.canUserRemoveQueue(admin.getId()));
		Assert.assertNotEquals(0,QueueSecurity.canUserRemoveQueue(user1.getId()));
		Assert.assertNotEquals(0,QueueSecurity.canUserRemoveQueue(user2.getId()));
	}
	
	@Test
	private void canUserSeeQueueRequests() {
		Assert.assertEquals(0,QueueSecurity.canUserSeeRequests(admin.getId()));
		Assert.assertNotEquals(0,QueueSecurity.canUserSeeRequests(user1.getId()));
		Assert.assertNotEquals(0,QueueSecurity.canUserSeeRequests(user2.getId()));
	}
	
	@Test
	private void canUserUpdateRequest() {
		String randomName=TestUtil.getRandomQueueName();
		Assert.assertEquals(0,QueueSecurity.canUserUpdateRequest(admin.getId(),randomName));
		//queues need unique names, so make sure we can't choose a name we already have
		Assert.assertNotEquals(0,QueueSecurity.canUserUpdateRequest(admin.getId(),Queues.getAll().get(0).getName()));
		Assert.assertNotEquals(0,QueueSecurity.canUserUpdateRequest(user1.getId(),randomName));
		Assert.assertNotEquals(0,QueueSecurity.canUserUpdateRequest(user2.getId(),randomName));
	}
	
	
	
	@Override
	protected String getTestName() {
		return "QueueSecurityTest";
	}

	@Override
	protected void setup() throws Exception {
		user1=ResourceLoader.loadUserIntoDatabase();
		user2=ResourceLoader.loadUserIntoDatabase();
		admin=Users.getAdmins().get(0);
		Assert.assertNotNull(user1);
		Assert.assertNotNull(user2);
		Assert.assertNotNull(admin);

	}

	@Override
	protected void teardown() throws Exception {
		Users.deleteUser(user1.getId(),admin.getId());
		Users.deleteUser(user2.getId(),admin.getId());
	}

}
