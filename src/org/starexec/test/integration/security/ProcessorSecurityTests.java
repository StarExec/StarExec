package org.starexec.test.integration.security;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.security.ProcessorSecurity;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;

public class ProcessorSecurityTests extends TestSequence {
	User admin = null;
	User owner = null;
	User otherUser = null;
	User notInCommunity = null;
	
	Space community = null;
	Processor p = null;
	
	@StarexecTest
	private void canUserSeeProcessorTest() {
		Assert.assertTrue(ProcessorSecurity.canUserSeeProcessor(p.getId(), admin.getId()).isSuccess());
		Assert.assertTrue(ProcessorSecurity.canUserSeeProcessor(p.getId(), owner.getId()).isSuccess());
		Assert.assertTrue(ProcessorSecurity.canUserSeeProcessor(p.getId(), otherUser.getId()).isSuccess());
		Assert.assertFalse(ProcessorSecurity.canUserSeeProcessor(p.getId(), notInCommunity.getId()).isSuccess());

	}
	
	@StarexecTest
	private void canUserSeeNoTypeProcessorTest() {
		Processor noType = Processors.getNoTypeProcessor();
		Assert.assertTrue(ProcessorSecurity.canUserSeeProcessor(noType.getId(), admin.getId()).isSuccess());
		Assert.assertTrue(ProcessorSecurity.canUserSeeProcessor(noType.getId(), owner.getId()).isSuccess());
		Assert.assertTrue(ProcessorSecurity.canUserSeeProcessor(noType.getId(), otherUser.getId()).isSuccess());
		Assert.assertTrue(ProcessorSecurity.canUserSeeProcessor(noType.getId(), notInCommunity.getId()).isSuccess());
	}
	
	@StarexecTest
	private void doesUserOwnProcessorTest() {
		Assert.assertTrue(ProcessorSecurity.doesUserOwnProcessor(p.getId(), admin.getId()).isSuccess());
		Assert.assertTrue(ProcessorSecurity.doesUserOwnProcessor(p.getId(), owner.getId()).isSuccess());
		Assert.assertFalse(ProcessorSecurity.doesUserOwnProcessor(p.getId(), otherUser.getId()).isSuccess());
		Assert.assertFalse(ProcessorSecurity.doesUserOwnProcessor(p.getId(), notInCommunity.getId()).isSuccess());
	}
	
	@StarexecTest
	private void canUserEditProcessorGoodAttrsTest() {
		Assert.assertTrue(ProcessorSecurity.canUserEditProcessor(p.getId(), admin.getId(), "test", "test").isSuccess());
		Assert.assertTrue(ProcessorSecurity.canUserEditProcessor(p.getId(), owner.getId(), "test", "test").isSuccess());
		Assert.assertFalse(ProcessorSecurity.canUserEditProcessor(p.getId(), otherUser.getId(), "test", "test").isSuccess());
		Assert.assertFalse(ProcessorSecurity.canUserEditProcessor(p.getId(), notInCommunity.getId(), "test", "test").isSuccess());
	}
	
	@StarexecTest
	private void canUserEditProcessorBadNameTest() {
		Assert.assertFalse(ProcessorSecurity.canUserEditProcessor(p.getId(), admin.getId(), "", "test").isSuccess());
		Assert.assertFalse(ProcessorSecurity.canUserEditProcessor(p.getId(), owner.getId(), "", "test").isSuccess());
		Assert.assertFalse(ProcessorSecurity.canUserEditProcessor(p.getId(), otherUser.getId(), "", "test").isSuccess());
		Assert.assertFalse(ProcessorSecurity.canUserEditProcessor(p.getId(), notInCommunity.getId(), "", "test").isSuccess());
	}
	
	@StarexecTest
	private void canUserEditProcessorBadDescTest() {
		Assert.assertFalse(ProcessorSecurity.canUserEditProcessor(p.getId(), admin.getId(), "test", TestUtil.getRandomAlphaString(R.SPACE_DESC_LEN+1)).isSuccess());
		Assert.assertFalse(ProcessorSecurity.canUserEditProcessor(p.getId(), owner.getId(), "test", TestUtil.getRandomAlphaString(R.SPACE_DESC_LEN+1)).isSuccess());
		Assert.assertFalse(ProcessorSecurity.canUserEditProcessor(p.getId(), otherUser.getId(), "test", TestUtil.getRandomAlphaString(R.SPACE_DESC_LEN+1)).isSuccess());
		Assert.assertFalse(ProcessorSecurity.canUserEditProcessor(p.getId(), notInCommunity.getId(), "test", TestUtil.getRandomAlphaString(R.SPACE_DESC_LEN+1)).isSuccess());
	}
	
	@Override
	protected String getTestName() {
		return "ProcessorSecurityTests";
	}

	@Override
	protected void setup() throws Exception {
		admin=Users.getAdmins().get(0);
		owner = loader.loadUserIntoDatabase();
		otherUser = loader.loadUserIntoDatabase();
		notInCommunity = loader.loadUserIntoDatabase();
		community=loader.loadSpaceIntoDatabase(owner.getId(), 1);
		Users.associate(otherUser.getId(),community.getId());
		p = loader.loadProcessorIntoDatabase(ProcessorType.POST, community.getId());
		
	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}

}
