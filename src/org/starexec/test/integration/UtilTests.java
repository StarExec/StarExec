package org.starexec.test.integration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.starexec.data.database.Communities;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.TestUtil;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;

public class UtilTests extends TestSequence{
	
	User u=null;
	Space s=null;
	Solver s1=null;
	Solver s2=null;
	
	@StarexecTest
	public void URLTest() {
		String random=TestUtil.getRandomSpaceName();
		String url=Util.url(random);
		Assert.assertNotNull(url);
		Assert.assertTrue(url.startsWith("http"));
		Assert.assertTrue(url.endsWith(random));
	}
	
	@StarexecTest 
	private void copyToSandboxTest() throws IOException {
		List<File> files=new ArrayList<File>();
		files.add(new File(s1.getPath()));
		files.add(new File(s2.getPath()));
		
		File sandbox=Util.copyFilesToNewSandbox(files);
		Assert.assertTrue(sandbox.exists());
		for (File f : files) {
			File sandboxFile=new File(sandbox,f.getName());
			Assert.assertTrue(sandboxFile.exists());
		}
		FileUtils.deleteQuietly(sandbox);
	}
	
	
	
	
	@Override
	protected String getTestName() {
		return "UtilTests";
	}

	@Override
	protected void setup() throws Exception {
		Space test=Communities.getTestCommunity();
		u=ResourceLoader.loadUserIntoDatabase();
		s=ResourceLoader.loadSpaceIntoDatabase(u.getId(), test.getId());
		s1=ResourceLoader.loadSolverIntoDatabase(s.getId(), u.getId());
		s2=ResourceLoader.loadSolverIntoDatabase(s.getId(), u.getId());

	}

	@Override
	protected void teardown() throws Exception {
		Assert.assertTrue(Solvers.deleteAndRemoveSolver(s1.getId()));
		Assert.assertTrue(Solvers.deleteAndRemoveSolver(s2.getId()));
		Assert.assertTrue(Spaces.removeSubspace(s.getId()));
		Assert.assertTrue(Users.deleteUser(u.getId(), Users.getAdmins().get(0).getId()));
	}

}
