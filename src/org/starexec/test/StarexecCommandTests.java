package org.starexec.test;

import org.junit.Assert;
import org.starexec.command.Connection;

import org.apache.log4j.Logger;
public class StarexecCommandTests extends TestSequence {
	private Connection con;
	public StarexecCommandTests() {
		setName("StarexecCommandTests");
	}
	
	@Test
	private void GetIDTest() throws Exception {
		int id=con.getUserID();
		if (id<0) {
			addMessage("User ID was incorrect");
			throw new Exception("failed");
		}
	}
	
	
	@Override
	protected void setup() throws Exception {
		//this prevents the apache http libraries from logging things. Their logs are very prolific
		//and drown out ours
		Logger.getLogger("org.apache.http").setLevel(org.apache.log4j.Level.OFF);
		con=new Connection("user@uiowa.edu","Starexec4ever","http://localhost:8080/starexec/");
		int status = con.login();
		Assert.assertEquals(0,status);
	}

	@Override
	protected void teardown() throws Exception {
		con.logout();

	}

}
