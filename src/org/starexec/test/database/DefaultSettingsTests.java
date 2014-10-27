package org.starexec.test.database;

import java.util.List;

import org.junit.Assert;
import org.starexec.data.database.Settings;
import org.starexec.data.database.Users;
import org.starexec.data.to.DefaultSettings;
import org.starexec.data.to.User;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.resources.ResourceLoader;

public class DefaultSettingsTests extends TestSequence {
	DefaultSettings settings=null;
	User u=null;
	User admin=null;
	@Override
	protected String getTestName() {
		return "DefaultSettingsTests";
	}
	
	@Test
	private void deleteSettingsTest() {
		DefaultSettings temp=ResourceLoader.loadDefaultSettingsProfileIntoDatabase(u.getId());
		Assert.assertTrue(Settings.deleteProfile(temp.getId()));
		Assert.assertNull(Settings.getProfileById(temp.getId()));
	}
	
	@Test
	private void getSettingsByNameTest() {
		List<DefaultSettings> settingsList=Settings.getDefaultSettingsByPrimIdAndType(settings.getPrimId(), settings.getType());
		
		Assert.assertEquals(settingsList.size(),1);
		Assert.assertTrue(settingsList.get(0).equals(settings));
	}
	
	
	@Test
	private void getSettingsTest() {
		DefaultSettings temp=Settings.getProfileById(settings.getId());
		Assert.assertNotNull(temp);
		Assert.assertTrue(temp.equals(settings));
	}
	
	
	@Override
	protected void setup() throws Exception {
		u=ResourceLoader.loadUserIntoDatabase();
		settings=ResourceLoader.loadDefaultSettingsProfileIntoDatabase(u.getId());
		admin=Users.getAdmins().get(0);
	}

	@Override
	protected void teardown() throws Exception {
		Assert.assertTrue(Users.deleteUser(u.getId(), admin.getId()));
		Assert.assertTrue(Settings.deleteProfile(settings.getId()));
		
	}

}
