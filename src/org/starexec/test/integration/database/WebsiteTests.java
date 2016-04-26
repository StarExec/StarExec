package org.starexec.test.integration.database;

import java.util.List;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.database.Websites;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.data.to.Website;
import org.starexec.data.to.Website.WebsiteType;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
/**
 * Tests for org.starexec.data.database.Websites.java
 * @author Eric
 */
public class WebsiteTests extends TestSequence {
	private Solver solver=null;
	private User user=null;
	private User admin=null;
	private Space space=null;
	
	@StarexecTest
	private void addAndRemoveSolverWebsite() {
		String name=TestUtil.getRandomUserName();
		Assert.assertTrue(Websites.add(solver.getId(), "http://www.uiowa.edu", name, WebsiteType.SOLVER));
		List<Website> sites=Websites.getAll(solver.getId(), WebsiteType.SOLVER);
		Integer id=websitesHaveName(sites,name);
		
		Assert.assertNotNull(id);
		
		Assert.assertTrue(Websites.delete(id));
		sites=Websites.getAll(solver.getId(), WebsiteType.SOLVER);

		id=websitesHaveName(sites,name);
		Assert.assertNull(id);
	}
	
	@StarexecTest
	private void addAndRemoveUserWebsite() {
		String name=TestUtil.getRandomUserName();
		Assert.assertTrue(Websites.add(user.getId(), "http://www.uiowa.edu", name, WebsiteType.USER));
		List<Website> sites=Websites.getAll(user.getId(), WebsiteType.USER);
		Integer id=websitesHaveName(sites,name);
		
		Assert.assertNotNull(id);
		
		Assert.assertTrue(Websites.delete(id));
		sites=Websites.getAll(user.getId(), WebsiteType.USER);

		id=websitesHaveName(sites,name);
		Assert.assertNull(id);
	}
	
	@StarexecTest
	private void getAllforHTMLTest() {
		List<Website> sites=Websites.getAllForHTML(solver.getId(), WebsiteType.SOLVER);
		Assert.assertNotNull(sites);
	}
	
	@StarexecTest
	private void addAndRemoveSpaceWebsite() {
		String name=TestUtil.getRandomUserName();
		Assert.assertTrue(Websites.add(space.getId(), "http://www.uiowa.edu", name, WebsiteType.SPACE));
		List<Website> sites=Websites.getAll(space.getId(), WebsiteType.SPACE);
		Integer id=websitesHaveName(sites,name);
		
		Assert.assertNotNull(id);
		
		Assert.assertTrue(Websites.delete(id));
		sites=Websites.getAll(space.getId(), WebsiteType.SPACE);

		id=websitesHaveName(sites,name);
		Assert.assertNull(id);
		
		
	}
	
	private Integer websitesHaveName(List<Website> sites, String name) {
		for (Website s : sites) {
			if (s.getName().equals(name)) {
				return s.getId();
			}
		}
		return null;
	}
	
	@Override
	protected String getTestName() {
		return "WebsiteTests";
	}

	@Override
	protected void setup() throws Exception {
		user=loader.loadUserIntoDatabase();
		admin=loader.loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa",R.ADMIN_ROLE_NAME);
		space=loader.loadSpaceIntoDatabase(user.getId(), Communities.getTestCommunity().getId());
		solver=loader.loadSolverIntoDatabase("CVC4.zip", space.getId(), user.getId());
		
	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}

}
