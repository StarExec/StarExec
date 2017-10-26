package org.starexec.test.integration.StateTests;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Spaces;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;

import java.util.HashSet;
import java.util.List;
public class IntroStateTests extends TestSequence {
	User admin=null;
	@Override
	protected String getTestName() {
		return "IntroStateTests";
	}

	@StarexecTest
	private void NoDefaultLeadersTest() {
		List<Space> spaces=Spaces.getAllSpaces();

		for (Space s : spaces) {
			Assert.assertFalse(Permissions.getSpaceDefault(s.getId()).isLeader());
		}
	}

	@StarexecTest
	private void UniqueSubspaceNamesTest() {
		List<Space> spaces=Spaces.getAllSpaces();
		for (Space s : spaces) {
			List<Space> subspaces=Spaces.getSubSpaces(s.getId());
			HashSet<String> names= new HashSet<>();
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
		admin=loader.loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa",R.ADMIN_ROLE_NAME);

	}

	@Override
	protected void teardown() throws Exception {

	}

}
