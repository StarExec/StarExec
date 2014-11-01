package org.starexec.test;

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
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;
import org.junit.Assert;

import com.mysql.jdbc.StringUtils;
public class UtilTests extends TestSequence{
	
	User u=null;
	Space s=null;
	Solver s1=null;
	Solver s2=null;
	@Test
	private void BytesToGigabytesTest() {
		Assert.assertEquals(1, Util.bytesToGigabytes(1073741824),.005);
		Assert.assertEquals(0,Util.bytesToGigabytes(0),.005);
	}
	
	@Test
	private void GetExtenstionTest() {
		Assert.assertEquals("zip",Util.getFileExtension("this/is/a/fake.zip"));
		Assert.assertEquals("test",Util.getFileExtension("fake.test"));
		
	}

	@Test
	private void GetTempPasswordTest() {
		int index=0;
		while (index<10) {
			index++;
			String pass=Util.getTempPassword();
			Assert.assertNotNull(pass);
			Assert.assertEquals(pass.length(),Util.clamp(6, 20, pass.length()));
	
		}
	}
	
	@Test
	private void ToIntegerListTest() {
		List<Integer> ints=Util.toIntegerList(new String[]{"11","2","321"});
		Assert.assertEquals(3,ints.size());
		
		Assert.assertEquals(11,(int)ints.get(0));
		Assert.assertEquals(2,(int)ints.get(1));
		Assert.assertEquals(321,(int)ints.get(2));
		
		ints=Util.toIntegerList(new String[]{});
		Assert.assertEquals(0,ints.size());

	}
	
	@Test
	private void URLTest() {
		String random=TestUtil.getRandomSpaceName();
		String url=Util.url(random);
		Assert.assertNotNull(url);
		Assert.assertTrue(url.startsWith("http"));
		Assert.assertTrue(url.endsWith(random));
	}
	
	@Test
	private void intClampTest() {
		Assert.assertEquals(1,Util.clamp(0, 10, 1));
		Assert.assertEquals(13,Util.clamp(13, 25, 7));
		Assert.assertEquals(30,Util.clamp(4, 30, 31));

		Assert.assertEquals(10,Util.clamp(10, 10, 10));
	}
	
	@Test
	private void longClampTest() {
	
		Assert.assertEquals(1,Util.clamp(0L, 10L, 1L));
		Assert.assertEquals(13,Util.clamp(13L, 25L, 7L));
		Assert.assertEquals(30,Util.clamp(4L, 30L, 31L));

		Assert.assertEquals(10,Util.clamp(10L, 10L, 10L));
		
	}
	
	@Test
	private void isNullOrEmptyTest() {
		Assert.assertTrue(Util.isNullOrEmpty(null));
		Assert.assertTrue(Util.isNullOrEmpty(""));
		Assert.assertFalse(Util.isNullOrEmpty("a"));
		Assert.assertFalse(Util.isNullOrEmpty("another test"));
		Assert.assertFalse(Util.isNullOrEmpty("null"));
		Assert.assertFalse(Util.isNullOrEmpty("empty"));
		
	}
	
	@Test
	private void BytesToMegabytesTest() {
		Assert.assertEquals(1, Util.bytesToMegabytes(1048576));
		Assert.assertEquals(1, Util.bytesToMegabytes(1048577));
		Assert.assertEquals(3, Util.bytesToMegabytes(4048576));
		
		Assert.assertEquals(0, Util.bytesToMegabytes(3));
	}
	
	@Test 
	private void copyToSandboxTest() throws IOException {
		List<File> files=new ArrayList<File>();
		files.add(new File(s1.getPath()));
		files.add(new File(s2.getPath()));
		
		File sandbox=Util.copyFilesToNewSandbox(files);
		Assert.assertEquals(files.size(),sandbox.listFiles().length);
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
		Assert.assertTrue(Spaces.removeSubspaces(s.getId(), u.getId()));
		Assert.assertTrue(Users.deleteUser(u.getId(), Users.getAdmins().get(0).getId()));
	}

}
