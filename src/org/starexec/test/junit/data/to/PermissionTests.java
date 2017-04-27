package org.starexec.test.junit.data.to;

import org.junit.Assert;
import org.junit.Test;
import org.starexec.data.to.Permission;

import java.util.List;

public class PermissionTests {

	private Permission getTestPermission() {
		Permission p = new Permission();
		p.setAddBenchmark(true);
		p.setAddJob(false);
		p.setAddSolver(true);
		p.setAddSpace(false);
		p.setAddUser(true);
		
		p.setRemoveBench(false);
		p.setRemoveJob(true);
		p.setRemoveSolver(false);
		p.setRemoveSpace(true);
		p.setRemoveUser(false);
		return p;
	}
	
	@Test
	public void getOffPermissionsTest() {
		Permission p = getTestPermission();
		List<String> perms = p.getOffPermissions();
		Assert.assertEquals(5, perms.size());
		for (String s : new String[] {"addJob", "addSpace", "removeBench", "removeSolver", "removeUser"}) {
			Assert.assertTrue(perms.contains(s));
		}
	}
	
	@Test
	public void getOnPermissionsTest() {
		Permission p = getTestPermission();
		List<String> perms = p.getOnPermissions();
		Assert.assertEquals(5, perms.size());
		for (String s : new String[] {"addBench", "addSolver", "addUser", "removeJob", "removeSpace"}) {
			Assert.assertTrue(perms.contains(s));
		}
	}
	
	@Test
	public void setPermissionOnTest() {
		
	}
}
