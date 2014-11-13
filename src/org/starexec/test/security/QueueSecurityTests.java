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
	private void ModifyQueuesTest() {
		Assert.assertEquals(true,QueueSecurity.canUserModifyQueues(admin.getId()).isSuccess());
		Assert.assertNotEquals(true,QueueSecurity.canUserModifyQueues(user1.getId()).isSuccess());
		Assert.assertNotEquals(true,QueueSecurity.canUserModifyQueues(user2.getId()).isSuccess());
	}
	
	@Test
	private void canUserMakeQueueTest() {
		String randomName=TestUtil.getRandomQueueName();
		Assert.assertEquals(true,QueueSecurity.canUserMakeQueue(admin.getId(), randomName).isSuccess());
		Assert.assertNotEquals(true,QueueSecurity.canUserMakeQueue(user1.getId(),randomName).isSuccess());
		Assert.assertNotEquals(true,QueueSecurity.canUserMakeQueue(user2.getId(),randomName).isSuccess());
	}
	
	
	
	
	
	@Test
	private void canUserUpdateRequest() {
		String randomName=TestUtil.getRandomQueueName();
		Assert.assertEquals(true,QueueSecurity.canUserUpdateRequest(admin.getId(),randomName).isSuccess());
		//queues need unique names, so make sure we can't choose a name we already have
		Assert.assertNotEquals(true,QueueSecurity.canUserUpdateRequest(admin.getId(),Queues.getAll().get(0).getName()).isSuccess());
		Assert.assertNotEquals(true,QueueSecurity.canUserUpdateRequest(user1.getId(),randomName).isSuccess());
		Assert.assertNotEquals(true,QueueSecurity.canUserUpdateRequest(user2.getId(),randomName).isSuccess());
	}
	
	@Test
	private void canEditQueueTest() {
		Assert.assertEquals(true,QueueSecurity.canUserEditQueue(admin.getId(), 50, 20).isSuccess());
		
		
		Assert.assertNotEquals(true,QueueSecurity.canUserEditQueue(user1.getId(), 50, 20).isSuccess());
		Assert.assertNotEquals(true,QueueSecurity.canUserEditQueue(admin.getId(), -1, 20).isSuccess());

		Assert.assertNotEquals(true,QueueSecurity.canUserEditQueue(admin.getId(), 50, 0).isSuccess());
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
