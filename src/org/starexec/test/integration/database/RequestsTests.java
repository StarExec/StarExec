package org.starexec.test.integration.database;

import java.util.UUID;

import org.junit.Assert;
import org.starexec.data.database.*;
import org.starexec.data.to.*;
import org.starexec.exceptions.*;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.DataTablesQuery;

/**
 * Tests for org.starexec.data.database.Requests.java
 * @author Eric
 */
public class RequestsTests extends TestSequence {

	User registeredUser=null;
	// requestedUser and requestedUser2 have requests for comm
	User requestedUser=null;
	// requestedUser's request
	CommunityRequest request=null;
	
	User admin=null;
	Space comm=null;
	Space comm2=null;
	
	
	@StarexecTest
	private void getCommunityRequestByUserTest() {
		CommunityRequest test=Requests.getCommunityRequest(request.getUserId());
		Assert.assertEquals(request.getCode(), test.getCode());
		Assert.assertEquals(request.getUserId(),test.getUserId());
		Assert.assertEquals(request.getMessage(),test.getMessage());
	}
	
	@StarexecTest
	private void getCommunityRequestByCodeTest() {
		CommunityRequest test=Requests.getCommunityRequest(request.getCode());
		Assert.assertEquals(request.getCode(), test.getCode());
		Assert.assertEquals(request.getUserId(),test.getUserId());
		Assert.assertEquals(request.getMessage(),test.getMessage());
	}

	@StarexecTest
	private void getCommunityRequestForCommunityTest() {
		int commRequestsSizeBefore = 0;
		int comm2RequestsSizeBefore = 0;
		int commRequestsSizeAfter = 0;
		int comm2RequestsSizeAfter = 0;
		DataTablesQuery query = new DataTablesQuery();
		query.setStartingRecord(0);
		query.setNumRecords(10);
		try {
			commRequestsSizeBefore = Requests.getPendingCommunityRequestsForCommunity(query, comm.getId()).size();
			comm2RequestsSizeBefore = Requests.getPendingCommunityRequestsForCommunity(query, comm2.getId()).size();
			User tempUser=ResourceLoader.loadUserIntoDatabase();
			ResourceLoader.loadCommunityRequestIntoDatabase(tempUser.getId(), comm.getId());
			commRequestsSizeAfter = Requests.getPendingCommunityRequestsForCommunity(query, comm.getId()).size();
			comm2RequestsSizeAfter = Requests.getPendingCommunityRequestsForCommunity(query, comm2.getId()).size();

			Assert.assertTrue(Users.deleteUser(tempUser.getId(), admin.getId()));
		} catch (StarExecDatabaseException e) {
			Assert.fail("Requests.getPendingCommunityRequestsForCommunity threw an exception: "+e.getMessage());
		} catch (StarExecSecurityException e) {
			Assert.fail(e.getMessage());
		}

		Assert.assertEquals(commRequestsSizeBefore, commRequestsSizeAfter-1);
		Assert.assertEquals(comm2RequestsSizeBefore, comm2RequestsSizeAfter);
	}

	
	@StarexecTest
	private void getCommunityRequestCountTest() {
		try {
			Assert.assertTrue(Requests.getCommunityRequestCount()>0);
		} catch (StarExecDatabaseException e) {
			Assert.fail("getCommunityRequestCount threw an exception."+e.getMessage());
		}
	}
	
	@StarexecTest
	private void approveCommunityRequestTest() {
		User tempUser=ResourceLoader.loadUserIntoDatabase();
		CommunityRequest tempRequest=ResourceLoader.loadCommunityRequestIntoDatabase(tempUser.getId(), comm.getId());
		Assert.assertFalse(Users.isMemberOfCommunity(tempUser.getId(), comm.getId()));

		Assert.assertTrue(Requests.approveCommunityRequest(tempUser.getId(), comm.getId()));
		Assert.assertNull(Requests.getCommunityRequest(tempRequest.getCode()));
		Assert.assertTrue(Users.isMemberOfCommunity(tempUser.getId(), comm.getId()));
		try {
			Assert.assertTrue(Users.deleteUser(tempUser.getId(), admin.getId()));
		} catch (StarExecSecurityException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@StarexecTest
	private void declineCommunityRequestTest() {
		User tempUser=ResourceLoader.loadUserIntoDatabase();
		CommunityRequest tempRequest=ResourceLoader.loadCommunityRequestIntoDatabase(tempUser.getId(), comm.getId());
		Assert.assertFalse(Users.isMemberOfCommunity(tempUser.getId(), comm.getId()));

		Assert.assertTrue(Requests.declineCommunityRequest(tempUser.getId(), comm.getId()));
		Assert.assertNull(Requests.getCommunityRequest(tempRequest.getCode()));
		Assert.assertFalse(Users.isMemberOfCommunity(tempUser.getId(), comm.getId()));
		try {
			Assert.assertTrue(Users.deleteUser(tempUser.getId(), admin.getId()));
		} catch (StarExecSecurityException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@StarexecTest
	private void addAndRedeemPassResetRequestTest() {
		String randomCode=UUID.randomUUID().toString();
		Assert.assertTrue(Requests.addPassResetRequest(registeredUser.getId(), randomCode));
		int userId=Requests.redeemPassResetRequest(randomCode);
		Assert.assertEquals(registeredUser.getId(),userId);
	}
	
	@StarexecTest
	private void addCommunityRequestTest() {
		String randomMessage=TestUtil.getRandomAlphaString(30);
		String randomCode=UUID.randomUUID().toString();
		Assert.assertTrue(Requests.addCommunityRequest(registeredUser, comm.getId(), randomCode, randomMessage));
		Assert.assertNotNull(Requests.getCommunityRequest(randomCode));
	}
	
	
	@Override
	protected String getTestName() {
		return "RequestsTests";
	}

	@Override
	protected void setup() throws Exception {
		registeredUser=ResourceLoader.loadUserIntoDatabase();
		requestedUser=ResourceLoader.loadUserIntoDatabase();
   
		admin=Users.getAdmins().get(0);
		comm=ResourceLoader.loadSpaceIntoDatabase(admin.getId(), 1);
		comm2=ResourceLoader.loadSpaceIntoDatabase(admin.getId(), 1);
		request= ResourceLoader.loadCommunityRequestIntoDatabase(requestedUser.getId(), comm.getId());
	}

	@Override
	protected void teardown() throws Exception {
		Spaces.removeSubspace(comm.getId());
		Spaces.removeSubspace(comm2.getId());
		Users.deleteUser(registeredUser.getId(), admin.getId());
		Users.deleteUser(requestedUser.getId(), admin.getId());
		
	}

}
