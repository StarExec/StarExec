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
		HashMap<Integer, Integer> users = new HashMap<Integer, Integer>();
		users.put(1, 0);
		monitor.setUsers(users);
		Assert.assertEquals(0, (long)monitor.getLoad(1));
		Assert.assertEquals(0, (long)monitor.getMin());
	}
	
	@Test
	public void addExistingUser() {
		HashMap<Integer, Integer> users = new HashMap<Integer, Integer>();
		users.put(1, 0);
		monitor.setUsers(users);
		monitor.changeLoad(1, 1);
		monitor.setUsers(users);
		Assert.assertEquals(1, (long)monitor.getLoad(1));
	}
	
	@Test
	public void testRemoveSingleUser() {
		HashMap<Integer, Integer> users = new HashMap<Integer, Integer>();
		users.put(1, 0);
		monitor.setUsers(users);
		monitor.changeLoad(1, 12);
		monitor.setUsers(new HashMap<Integer, Integer>());
		
		Assert.assertEquals(null, monitor.getLoad(1));
		Assert.assertEquals(null, monitor.getMin());
	}
	
	@Test
	public void testRemoveMinUser() {
		HashMap<Integer, Integer> users = new HashMap<Integer, Integer>();
		users.put(1, 0);
		users.put(2, 0);
		monitor.setUsers(users);
		monitor.changeLoad(1, 3);
		Assert.assertEquals(0, (long)monitor.getMin());
		users.remove(2);
		monitor.setUsers(users);

		Assert.assertEquals(3, (long)monitor.getMin());
	}
	
	@Test
	public void testIncrementLoadSingleUser() {
		HashMap<Integer, Integer> users = new HashMap<Integer, Integer>();
		users.put(1, 0);
		monitor.setUsers(users);
		monitor.changeLoad(1, 6);
		Assert.assertEquals(6, (long)monitor.getLoad(1));
		Assert.assertEquals(6, (long)monitor.getMin());

		monitor.changeLoad(1, 7);
		Assert.assertEquals(13, (long)monitor.getLoad(1));
		Assert.assertEquals(13, (long)monitor.getMin());

	}
	
	@Test
	public void testIncrementLoadTwoUsers() {
		HashMap<Integer, Integer> users = new HashMap<Integer, Integer>();
		users.put(1, 0);
		users.put(2, 0);
		monitor.setUsers(users);
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
		users.put(2, 3);
		users.put(3, 3);

		monitor.setUsers(users);

		for (Integer i : users.keySet()) {
			Assert.assertEquals(3, (long)monitor.getLoad(i));
		}
		Assert.assertEquals(3, (long)monitor.getMin());
	}
	
	@Test
	public void testSetUsersNoOverlap() {
		HashMap<Integer, Integer> users = new HashMap<Integer, Integer>();
		users.put(4, 0);
		monitor.setUsers(users);
		monitor.changeLoad(4, 5);
		users = new HashMap<Integer, Integer>();
		users.put(1, 3);
		users.put(2, 3);
		users.put(3, 3);
		monitor.setUsers(users);
		for (Integer i : users.keySet()) {
			Assert.assertEquals(3, (long)monitor.getLoad(i));
		}
		Assert.assertEquals(null, monitor.getLoad(4));
		Assert.assertEquals(3, (long)monitor.getMin());
	}
	
	@Test
	public void testSetUsersAllOverlap() {
		HashMap<Integer, Integer> users = new HashMap<Integer, Integer>();
		users.put(1, 0);
		users.put(2, 0);
		monitor.setUsers(users);
		monitor.changeLoad(1, 2);
		monitor.changeLoad(2, 2);
		users = new HashMap<Integer, Integer>();
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
		HashMap<Integer, Integer> users = new HashMap<Integer, Integer>();
		users.put(1, 0);
		monitor.setUsers(users);
		Assert.assertFalse(monitor.skipUser(1));
		monitor.changeLoad(1, 3000);
		Assert.assertFalse(monitor.skipUser(1));
	}
	
	@Test
	public void skipUserTestTwoUsers() {
		HashMap<Integer, Integer> users = new HashMap<Integer, Integer>();
		users.put(1, 0);
		users.put(2, 0);
		monitor.setUsers(users);
		monitor.changeLoad(1, 1);
		Assert.assertFalse(monitor.skipUser(1));
		Assert.assertFalse(monitor.skipUser(2));
		monitor.changeLoad(1, 2000);
		Assert.assertTrue(monitor.skipUser(1));
		Assert.assertFalse(monitor.skipUser(2));
	}
	
	@Test
	public void testAddUserAfterLoad() {
		HashMap<Integer, Integer> users = new HashMap<Integer, Integer>();
		users.put(1, 0);
		monitor.setUsers(users);
		monitor.changeLoad(1, 5);
		users.put(2, 0);
		monitor.setUsers(users);
		Assert.assertEquals(5, (long)monitor.getLoad(1));
		Assert.assertEquals(5, (long)monitor.getLoad(2));
		Assert.assertEquals(5, (long)monitor.getMin());
	}
}
