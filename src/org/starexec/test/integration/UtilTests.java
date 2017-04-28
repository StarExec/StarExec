package org.starexec.test.integration;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.starexec.data.database.Communities;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.TestUtil;
import org.starexec.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
		List<File> files= new ArrayList<>();
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
		u=loader.loadUserIntoDatabase();
		s=loader.loadSpaceIntoDatabase(u.getId(), test.getId());
		s1=loader.loadSolverIntoDatabase(s.getId(), u.getId());
		s2=loader.loadSolverIntoDatabase(s.getId(), u.getId());

	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}

}
