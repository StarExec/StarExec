package org.starexec.test.security;

import org.junit.Assert;
import org.starexec.data.database.Settings;
import org.starexec.data.database.Users;
import org.starexec.data.security.SettingSecurity;
import org.starexec.data.to.DefaultSettings;
import org.starexec.data.to.User;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.resources.ResourceLoader;

public class SettingSecurityTests extends TestSequence {
	User u=null;
	User u2=null;
	User admin=null;
	DefaultSettings s=null;
	@Override
	protected String getTestName() {
		return "SettingSecurityTests";
	}
	
	@Test
	private void canModifySettingsTest() {
		Assert.assertTrue(SettingSecurity.canModifySettings(s.getId(), u.getId()).isSuccess());
		Assert.assertTrue(SettingSecurity.canModifySettings(s.getId(), admin.getId()).isSuccess());
		Assert.assertFalse(SettingSecurity.canModifySettings(s.getId(), u2.getId()).isSuccess());
		Assert.assertFalse(SettingSecurity.canModifySettings(-1, u2.getId()).isSuccess());
	}
	
	@Test
	private void canUpdateSettingsTest() {
		//SettingSecurity.canUpdateSettings(id, attribute, newValue, userId)
	}

	@Override
	protected void setup() throws Exception {
		u=ResourceLoader.loadUserIntoDatabase();
		u2=ResourceLoader.loadUserIntoDatabase();
		s=ResourceLoader.loadDefaultSettingsProfileIntoDatabase(u.getId());
		admin=Users.getAdmins().get(0);
	}

	@Override
	protected void teardown() throws Exception {
		Settings.deleteProfile(s.getId());

		Users.deleteUser(u.getId(), admin.getId());
		Users.deleteUser(u2.getId(), admin.getId());

		
	}

}
