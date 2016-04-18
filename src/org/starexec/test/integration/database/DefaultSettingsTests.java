package org.starexec.test.integration.database;

import java.util.List;

import org.junit.Assert;
import org.starexec.data.database.Settings;
import org.starexec.data.database.Users;
import org.starexec.data.to.DefaultSettings;
import org.starexec.data.to.User;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;

/**
 * Tests for org.starexec.data.database.Settings.java
 * @author Eric
 */
public class DefaultSettingsTests extends TestSequence {
	DefaultSettings settings=null;
	DefaultSettings settings2=null;
	User u=null;
	User u2=null;
	
	User admin=null;
	@Override
	protected String getTestName() {
		return "DefaultSettingsTests";
	}
	
	@StarexecTest
	private void getSettingsByUser() {
		List<DefaultSettings> settingsList=Settings.getDefaultSettingsOwnedByUser(u.getId());
		Assert.assertEquals(settingsList.size(), 1);
		Assert.assertTrue(settingsList.get(0).equals(settings));
	}
	
	@StarexecTest
	private void deleteSettingsTest() {
		DefaultSettings temp=loader.loadDefaultSettingsProfileIntoDatabase(u.getId());
		Assert.assertTrue(Settings.deleteProfile(temp.getId()));
		Assert.assertNull(Settings.getProfileById(temp.getId()));
	}
	
	@StarexecTest
	private void getSettingsByNameTest() {
		List<DefaultSettings> settingsList=Settings.getDefaultSettingsByPrimIdAndType(settings.getPrimId(), settings.getType());
		
		Assert.assertEquals(settingsList.size(),1);
		Assert.assertTrue(settingsList.get(0).equals(settings));
	}
	
	
	@StarexecTest
	private void getSettingsTest() {
		DefaultSettings temp=Settings.getProfileById(settings.getId());
		Assert.assertNotNull(temp);
		Assert.assertTrue(temp.equals(settings));
	}
	
	@StarexecTest
	private void updateDefaultMemoryLimitTest() {

		
		Assert.assertTrue(Settings.setDefaultMaxMemory(settings.getId(), settings.getMaxMemory()+1));
		Assert.assertEquals(settings.getMaxMemory()+1, Settings.getProfileById(settings.getId()).getMaxMemory());
		Assert.assertTrue(Settings.setDefaultMaxMemory(settings.getId(), settings.getMaxMemory()));
	}
	
	@Override
	protected void setup() throws Exception {
		u=loader.loadUserIntoDatabase();
		u2=loader.loadUserIntoDatabase();
		
		settings=loader.loadDefaultSettingsProfileIntoDatabase(u.getId());
		settings2=loader.loadDefaultSettingsProfileIntoDatabase(u2.getId());
		admin=Users.getAdmins().get(0);
	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}

}
