package org.starexec.test.integration.security;

import org.junit.Assert;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Users;
import org.starexec.data.security.QueueSecurity;
import org.starexec.data.to.User;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;

public class QueueSecurityTests extends TestSequence {

	User user1=null;
	User user2=null;
	User admin=null;
	
	@StarexecTest
	private void canUserMakeQueueTest() {
		String randomName=TestUtil.getRandomQueueName();
		Assert.assertEquals(true,QueueSecurity.canUserMakeQueue(admin.getId(), randomName).isSuccess());
		Assert.assertNotEquals(true,QueueSecurity.canUserMakeQueue(user1.getId(),randomName).isSuccess());
		Assert.assertNotEquals(true,QueueSecurity.canUserMakeQueue(user2.getId(),randomName).isSuccess());
	}
	
	@StarexecTest
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
		Users.deleteUser(user1.getId());
		Users.deleteUser(user2.getId());
	}

}
