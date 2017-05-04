package org.starexec.test.integration.security;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Websites;
import org.starexec.data.security.WebsiteSecurity;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.data.to.Website.WebsiteType;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;

/**
 * Contains tests for the class WebsiteSecurity.java
 * @author Eric
 *
 */
public class WebsiteSecurityTests extends TestSequence {
	User owner=null;
	User nonOwner=null;
	User admin=null;
	Space space=null;
	Solver solver = null;
	@StarexecTest
	private void canAddWebsiteBadNameTest() {
		Assert.assertFalse(WebsiteSecurity.canUserAddWebsite(space.getId(),R.SPACE, owner.getId(),"<script>","http://www.fake.com").isSuccess());
		Assert.assertFalse(WebsiteSecurity.canUserAddWebsite(space.getId(),R.SPACE, admin.getId(),"<script>","http://www.fake.com").isSuccess());
	}
	
	@StarexecTest
	private void canAddWebsiteBadURLTest() {
		Assert.assertFalse(WebsiteSecurity.canUserAddWebsite(space.getId(),R.SPACE, owner.getId(),"new","<script>").isSuccess());
		Assert.assertFalse(WebsiteSecurity.canUserAddWebsite(space.getId(),R.SPACE, admin.getId(),"new","<script>").isSuccess());
	}
	
	@StarexecTest
	private void canAddWebsiteBadTypeTest() {
		Assert.assertFalse(WebsiteSecurity.canUserAddWebsite(space.getId(),R.JOB, owner.getId(),"new","http://www.fake.com").isSuccess());
		Assert.assertFalse(WebsiteSecurity.canUserAddWebsite(space.getId(),R.JOB, admin.getId(),"new","http://www.fake.com").isSuccess());
	}
	
	@StarexecTest
	private void CanAssociateSpaceWebsiteTest() {
		Assert.assertTrue(WebsiteSecurity.canUserAddWebsite(space.getId(),R.SPACE, owner.getId(),"new","http://www.fake.com").isSuccess());
		Assert.assertTrue(WebsiteSecurity.canUserAddWebsite(space.getId(),R.SPACE, admin.getId(),"new","http://www.fake.com").isSuccess());
		
		Assert.assertFalse(WebsiteSecurity.canUserAddWebsite(space.getId(),R.SPACE, nonOwner.getId(),"new","http://www.fake.com").isSuccess());
	}
	
	@StarexecTest
	private void CanDeleteSpaceWebsiteTest() {
		Websites.add(space.getId(), "https://www.fake.edu", "new", WebsiteType.SPACE);
		int websiteId=Websites.getAll(space.getId(), WebsiteType.SPACE).get(0).getId();
		Assert.assertTrue(WebsiteSecurity.canUserDeleteWebsite(websiteId, owner.getId()).isSuccess());
		Assert.assertTrue(WebsiteSecurity.canUserDeleteWebsite(websiteId, admin.getId()).isSuccess());

		Assert.assertFalse(WebsiteSecurity.canUserDeleteWebsite(websiteId, nonOwner.getId()).isSuccess());
		
		Assert.assertFalse(WebsiteSecurity.canUserDeleteWebsite(-1, owner.getId()).isSuccess());
		Assert.assertFalse(WebsiteSecurity.canUserDeleteWebsite(-1, admin.getId()).isSuccess());
	}
	
	@StarexecTest
	private void CanAssociateSolverWebsiteTest() {
		Assert.assertTrue(WebsiteSecurity.canUserAddWebsite(solver.getId(),R.SOLVER, owner.getId(),"new","http://www.fake.com").isSuccess());
		Assert.assertTrue(WebsiteSecurity.canUserAddWebsite(solver.getId(),R.SOLVER, admin.getId(),"new","http://www.fake.com").isSuccess());
		
		Assert.assertFalse(WebsiteSecurity.canUserAddWebsite(solver.getId(),R.SOLVER, nonOwner.getId(),"new","http://www.fake.com").isSuccess());
	}
	
	@StarexecTest
	private void CanDeleteSolverWebsiteTest() {
		Websites.add(solver.getId(), "https://www.fake.edu", "new", WebsiteType.SOLVER);
		int websiteId=Websites.getAll(solver.getId(), WebsiteType.SOLVER).get(0).getId();
		Assert.assertTrue(WebsiteSecurity.canUserDeleteWebsite(websiteId, owner.getId()).isSuccess());
		Assert.assertTrue(WebsiteSecurity.canUserDeleteWebsite(websiteId, admin.getId()).isSuccess());
		
		Assert.assertFalse(WebsiteSecurity.canUserDeleteWebsite(websiteId, nonOwner.getId()).isSuccess());
		Assert.assertFalse(WebsiteSecurity.canUserDeleteWebsite(-1, owner.getId()).isSuccess());
		Assert.assertFalse(WebsiteSecurity.canUserDeleteWebsite(-1, admin.getId()).isSuccess());
	}
	
	@StarexecTest
	private void CanAssociateUserWebsiteTest() {
		Assert.assertTrue(WebsiteSecurity.canUserAddWebsite(owner.getId(),R.USER, owner.getId(),"new","http://www.fake.com").isSuccess());
		Assert.assertTrue(WebsiteSecurity.canUserAddWebsite(owner.getId(),R.USER, admin.getId(),"new","http://www.fake.com").isSuccess());
		
		Assert.assertFalse(WebsiteSecurity.canUserAddWebsite(owner.getId(),R.USER, nonOwner.getId(),"new","http://www.fake.com").isSuccess());
	}
	
	@StarexecTest
	private void CanDeleteUserWebsiteTest() {
		Websites.add(owner.getId(), "https://www.fake.edu", "new", WebsiteType.USER);
		int websiteId=Websites.getAll(owner.getId(), WebsiteType.USER).get(0).getId();
		Assert.assertTrue(WebsiteSecurity.canUserDeleteWebsite(websiteId, owner.getId()).isSuccess());
		Assert.assertTrue(WebsiteSecurity.canUserDeleteWebsite(websiteId, admin.getId()).isSuccess());
		
		Assert.assertFalse(WebsiteSecurity.canUserDeleteWebsite(websiteId, nonOwner.getId()).isSuccess());
		Assert.assertFalse(WebsiteSecurity.canUserDeleteWebsite(-1, owner.getId()).isSuccess());
		Assert.assertFalse(WebsiteSecurity.canUserDeleteWebsite(-1, admin.getId()).isSuccess());
	}
	
	@Override
	protected String getTestName() {
		return "WebsiteSecurityTests";
	}

	@Override
	protected void setup() throws Exception {
		owner=loader.loadUserIntoDatabase();
		nonOwner=loader.loadUserIntoDatabase();
		admin=loader.loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa",R.ADMIN_ROLE_NAME);
		space=loader.loadSpaceIntoDatabase(owner.getId(), 1);
		solver=loader.loadSolverIntoDatabase(space.getId(), owner.getId());
	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}

}
