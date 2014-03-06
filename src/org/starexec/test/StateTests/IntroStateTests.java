package org.starexec.test.StateTests;

import java.util.List;

import org.junit.Assert;

import org.starexec.servlets.BenchmarkUploader;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Spaces;
import org.starexec.data.to.Space;
public class IntroStateTests extends TestSequence {

	@Override
	protected String getTestName() {
		return "IntroStateTests";
	}
	
	@Test
	private void NoDefaultLeadersTest() {
		List<Space> spaces=Spaces.GetAllSpaces();
		
		for (Space s : spaces) {
			Assert.assertFalse(Permissions.getSpaceDefault(s.getId()).isLeader());
		}
	}

	@Override
	protected void setup() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void teardown() throws Exception {
		// TODO Auto-generated method stub
		
	}

}
