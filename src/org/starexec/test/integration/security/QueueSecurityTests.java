package org.starexec.test.integration.security;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.security.QueueSecurity;
import org.starexec.data.to.User;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;

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
		user1=loader.loadUserIntoDatabase();
		user2=loader.loadUserIntoDatabase();
		admin=loader.loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa",R.ADMIN_ROLE_NAME);
		Assert.assertNotNull(user1);
		Assert.assertNotNull(user2);
		Assert.assertNotNull(admin);

	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}

}
