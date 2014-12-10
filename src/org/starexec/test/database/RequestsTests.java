package org.starexec.test.database;

import java.util.UUID;

import org.junit.Assert;
import org.starexec.data.database.*;
import org.starexec.data.to.*;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.TestUtil;
import org.starexec.test.resources.ResourceLoader;

public class RequestsTests extends TestSequence {

	User registeredUser=null;
	User requestedUser=null;
	CommunityRequest request=null;
	User admin=null;
	Space comm=null;
	
	
	@Test
	private void getCommunityRequestByUserTest() {
		CommunityRequest test=Requests.getCommunityRequest(request.getUserId());
		Assert.assertEquals(request.getCode(), test.getCode());
		Assert.assertEquals(request.getUserId(),test.getUserId());
		Assert.assertEquals(request.getMessage(),test.getMessage());
	}
	
	@Test
	private void getCommunityRequestByCodeTest() {
		CommunityRequest test=Requests.getCommunityRequest(request.getCode());
		Assert.assertEquals(request.getCode(), test.getCode());
		Assert.assertEquals(request.getUserId(),test.getUserId());
		Assert.assertEquals(request.getMessage(),test.getMessage());
	}
	
	@Test
	private void getCommunityRequestCountTest() {
		Assert.assertTrue(Requests.getCommunityRequestCount()>0);
	}
	
	@Test
	private void approveCommunityRequestTest() {
		User tempUser=ResourceLoader.loadUserIntoDatabase();
		CommunityRequest tempRequest=ResourceLoader.loadCommunityRequestIntoDatabase(tempUser.getId(), comm.getId());
		Assert.assertFalse(Users.isMemberOfCommunity(tempUser.getId(), comm.getId()));

		Assert.assertTrue(Requests.approveCommunityRequest(tempUser.getId(), comm.getId()));
		Assert.assertNull(Requests.getCommunityRequest(tempRequest.getCode()));
		Assert.assertTrue(Users.isMemberOfCommunity(tempUser.getId(), comm.getId()));
		Assert.assertTrue(Users.deleteUser(tempUser.getId(), admin.getId()));
	}
	
	@Test
	private void declineCommunityRequestTest() {
		User tempUser=ResourceLoader.loadUserIntoDatabase();
		CommunityRequest tempRequest=ResourceLoader.loadCommunityRequestIntoDatabase(tempUser.getId(), comm.getId());
		Assert.assertFalse(Users.isMemberOfCommunity(tempUser.getId(), comm.getId()));

		Assert.assertTrue(Requests.declineCommunityRequest(tempUser.getId(), comm.getId()));
		Assert.assertNull(Requests.getCommunityRequest(tempRequest.getCode()));
		Assert.assertFalse(Users.isMemberOfCommunity(tempUser.getId(), comm.getId()));
		Assert.assertTrue(Users.deleteUser(tempUser.getId(), admin.getId()));
	}
	
	@Test
	private void addAndRedeemPassResetRequestTest() {
		String randomCode=UUID.randomUUID().toString();
		Assert.assertTrue(Requests.addPassResetRequest(registeredUser.getId(), randomCode));
		int userId=Requests.redeemPassResetRequest(randomCode);
		Assert.assertEquals(registeredUser.getId(),userId);
	}
	
	@Test
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
		request= ResourceLoader.loadCommunityRequestIntoDatabase(requestedUser.getId(), comm.getId());
	}

	@Override
	protected void teardown() throws Exception {
		Spaces.removeSubspaces(comm.getId(), admin.getId());
		Users.deleteUser(registeredUser.getId(), admin.getId());
		Users.deleteUser(requestedUser.getId(), admin.getId());
		
	}

}
