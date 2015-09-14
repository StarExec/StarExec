package org.starexec.test.junit.jobs;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.starexec.jobs.LoadBalanceMonitor;

public class LoadBalanceMonitorTests {
	
	private LoadBalanceMonitor monitor = null;
	@Before
	public void setup() {
		monitor = new LoadBalanceMonitor();
	}
	
	@Test
	public void testAddSingleUser() {
		monitor.addUser(1, 0);
		Assert.assertEquals(0, (long)monitor.getLoad(1));
		Assert.assertEquals(0, (long)monitor.getMin());
	}
	
	@Test
	public void addExistingUser() {
		monitor.addUser(1, 1);
		monitor.changeLoad(1, 1);
		monitor.addUser(1, 0);
		Assert.assertEquals(2, (long)monitor.getLoad(1));
	}
	
	@Test
	public void testRemoveSingleUser() {
		monitor.addUser(1, 0);
		monitor.changeLoad(1, 12);
		monitor.removeUser(1);
		
		Assert.assertEquals(null, monitor.getLoad(1));
		Assert.assertEquals(null, monitor.getMin());
	}
	
	@Test
	public void testRemoveMinUser() {
		monitor.addUser(1, 0);
		monitor.addUser(2, 0);
		monitor.changeLoad(1, 3);
		Assert.assertEquals(0, (long)monitor.getMin());
		monitor.removeUser(2);
		Assert.assertEquals(3, (long)monitor.getMin());
	}
	
	@Test
	public void testIncrementLoadSingleUser() {
		monitor.addUser(1, 0);
		monitor.changeLoad(1, 6);
		Assert.assertEquals(6, (long)monitor.getLoad(1));
		Assert.assertEquals(6, (long)monitor.getMin());

		monitor.changeLoad(1, 7);
		Assert.assertEquals(13, (long)monitor.getLoad(1));
		Assert.assertEquals(13, (long)monitor.getMin());

	}
	
	@Test
	public void testIncrementLoadTwoUsers() {
		monitor.addUser(1, 0);
		monitor.addUser(2, 0);
		monitor.changeLoad(1, 6);
		Assert.assertEquals(6, (long)monitor.getLoad(1));
		Assert.assertEquals(0, (long)monitor.getLoad(2));
		Assert.assertEquals(0, (long)monitor.getMin());

		monitor.changeLoad(2, 7);
		Assert.assertEquals(6, (long)monitor.getLoad(1));
		Assert.assertEquals(7, (long)monitor.getLoad(2));
		Assert.assertEquals(6, (long)monitor.getMin());
	}
	
	@Test
	public void testSetUsersEmptyMonitor() {
		HashMap<Integer, Integer> users = new HashMap<Integer, Integer>();
		users.put(1, 3);
		users.put(2, 0);
		users.put(3, 0);

		monitor.setUsers(users);

		for (Integer i : users.keySet()) {
			Assert.assertEquals(3, (long)monitor.getLoad(i));
		}
		Assert.assertEquals(3, (long)monitor.getMin());
	}
	
	@Test
	public void testSetUsersNoOverlap() {
		monitor.addUser(4, 1);
		monitor.changeLoad(4, 5);
		HashMap<Integer, Integer> users = new HashMap<Integer, Integer>();
		users.put(1, 3);
		users.put(2, 0);
		users.put(3, 0);
		monitor.setUsers(users);
		for (Integer i : users.keySet()) {
			Assert.assertEquals(3, (long)monitor.getLoad(i));
		}
		Assert.assertEquals(null, monitor.getLoad(4));
		Assert.assertEquals(3, (long)monitor.getMin());
	}
	
	@Test
	public void testSetUsersAllOverlap() {
		monitor.addUser(1, 0);
		monitor.addUser(2, 0);
		monitor.changeLoad(1, 2);
		monitor.changeLoad(2, 2);
		HashMap<Integer, Integer> users = new HashMap<Integer, Integer>();
		users.put(1, 3);
		users.put(2, 3);
		monitor.setUsers(users);
		for (Integer i : users.keySet()) {
			Assert.assertEquals(2, (long)monitor.getLoad(i));
		}
		Assert.assertEquals(2, (long)monitor.getMin());
	}
	
	@Test
	public void skipUserTestSingleUser() {
		monitor.addUser(1, 0);
		Assert.assertFalse(monitor.skipUser(1));
		monitor.changeLoad(1, 3000);
		Assert.assertFalse(monitor.skipUser(1));
	}
	
	@Test
	public void skipUserTestTwoUsers() {
		monitor.addUser(1, 0);
		monitor.addUser(2, 0);
		monitor.changeLoad(1, 1);
		Assert.assertFalse(monitor.skipUser(1));
		Assert.assertFalse(monitor.skipUser(2));
		monitor.changeLoad(1, 2000);
		Assert.assertTrue(monitor.skipUser(1));
		Assert.assertFalse(monitor.skipUser(2));
	}
	
	@Test
	public void testAddUserAfterLoad() {
		monitor.addUser(1, 0);
		monitor.changeLoad(1, 5);
		monitor.addUser(2, 0);
		Assert.assertEquals(5, (long)monitor.getLoad(1));
		Assert.assertEquals(5, (long)monitor.getLoad(2));
		Assert.assertEquals(5, (long)monitor.getMin());
	}
}
