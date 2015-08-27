package org.starexec.test.integration.StateTests;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.starexec.servlets.BenchmarkUploader;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.util.Util;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Job;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
public class IntroStateTests extends TestSequence {
	User admin=null;
	@Override
	protected String getTestName() {
		return "IntroStateTests";
	}
	
	@StarexecTest
	private void NoDefaultLeadersTest() {
		List<Space> spaces=Spaces.GetAllSpaces();
		
		for (Space s : spaces) {
			Assert.assertFalse(Permissions.getSpaceDefault(s.getId()).isLeader());
		}
	}
	
	@StarexecTest 
	private void UniqueSubspaceNamesTest() {
		List<Space> spaces=Spaces.GetAllSpaces();
		for (Space s : spaces) {
			List<Space> subspaces=Spaces.getSubSpaces(s.getId(), admin.getId());
			HashSet<String> names=new HashSet<String>();
			for (Space sub : subspaces) {
				if (names.contains(sub.getName())) {
					addMessage("ERROR: space id = "+sub.getId() +" has a duplicate name in space "+s.getId());
				}
				Assert.assertFalse(names.contains(sub.getName()));
				names.add(sub.getName());
			}
		}
	}


	@Override
	protected void setup() throws Exception {
		admin=Users.getAdmins().get(0);
		
	}

	@Override
	protected void teardown() throws Exception {
		
	}

}
