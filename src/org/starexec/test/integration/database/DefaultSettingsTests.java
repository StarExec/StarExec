package org.starexec.test.integration.database;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Settings;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.DefaultSettings;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;

/**
 * Tests for org.starexec.data.database.Settings.java
 * @author Eric
 */
public class DefaultSettingsTests extends TestSequence {
	DefaultSettings settings=null;
	DefaultSettings settings2=null;
	User u=null;
	User u2=null;

	List<Integer> benchIds;

	Space space = null;
	
	User admin=null;
	@Override
	protected String getTestName() {
		return "DefaultSettingsTests";
	}

	@StarexecTest
    private void addDefaultBenchmarkTest() {
        DefaultSettings newSettings = DefaultSettings.copy(settings);
        newSettings.addBenchId(benchIds.get(0));
        Settings.addNewSettingsProfile(settings);

        try {
            int benchIdToAdd = benchIds.get(1);
            Settings.addDefaultBenchmark(newSettings.getId(), benchIdToAdd);
            List<Benchmark> benchmarks = Settings.getDefaultBenchmarks(settings.getId());

            Assert.assertTrue("Benchmark was not part of default benchmarks.",
                    benchmarks
                            .stream()
                            .anyMatch(b -> b.getId() == benchIdToAdd));
        } catch (SQLException e) {
            Assert.fail("Caught SQLException: " + Util.getStackTrace(e));
        } finally {
            Settings.deleteProfile(newSettings.getId());
        }
    }

	@StarexecTest
	private void addSettingsTest() {
		DefaultSettings newSettings = DefaultSettings.copy(settings);
		for (Integer benchId : benchIds) {
			newSettings.addBenchId(benchId);
		}

		Settings.addNewSettingsProfile(settings);

        try {
            DefaultSettings dbSettings = Settings.getProfileById(newSettings.getId());

            Assert.assertEquals( "PrimId was not equal.", settings.getPrimId(), dbSettings.getPrimId());
            Assert.assertEquals( "PreProcessorId was not equal.", settings.getPreProcessorId(), dbSettings.getPreProcessorId());
            Assert.assertEquals( "PostProcessorId was not equal.", settings.getPostProcessorId(), dbSettings.getPostProcessorId());
            Assert.assertEquals( "BenchProcessorId was not equal.", settings.getBenchProcessorId(), dbSettings.getBenchProcessorId());
            Assert.assertEquals( "SolverId was not equal.", settings.getSolverId(), dbSettings.getSolverId());
            Assert.assertEquals( "WallclockTimeout was not equal.", settings.getWallclockTimeout(), dbSettings.getWallclockTimeout());
            Assert.assertEquals( "CpuTimeout was not equal.", settings.getCpuTimeout(), dbSettings.getCpuTimeout());
            Assert.assertEquals( "MaxMemory was not equal.", settings.getMaxMemory(), dbSettings.getMaxMemory());
            Assert.assertEquals( "DependenciesEnabled was not equal.", settings.isDependenciesEnabled(), dbSettings.isDependenciesEnabled());
            Assert.assertEquals( "Name was not equal.", settings.getName(), dbSettings.getName());
            Assert.assertEquals( "Type was not equal.", settings.getType(), dbSettings.getType());

            List<Integer> settingsBenchIds = settings.getBenchIds();
            List<Integer> dbSettingsBenchIds = dbSettings.getBenchIds();

            final String benchIdsNotEqual = "Bench ids were not equal.";
            Assert.assertEquals(benchIdsNotEqual, settingsBenchIds.size(), dbSettingsBenchIds.size());

            Collections.sort(benchIds);
            Collections.sort(dbSettingsBenchIds);
            for (int i = 0; i < benchIds.size(); i++) {
                Assert.assertEquals(benchIdsNotEqual, settingsBenchIds.get(i), dbSettingsBenchIds.get(i));
            }




        } catch(SQLException e) {
            Assert.fail("Caught SQLException: " + Util.getStackTrace(e));
        } finally {
            Settings.deleteProfile(newSettings.getId());
        }

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
		try {
			Assert.assertNull(Settings.getProfileById(temp.getId()));
		} catch (SQLException e) {
			Assert.fail("Caught SQLException: "+ Util.getStackTrace(e));
		}
	}
	
	@StarexecTest
	private void getSettingsByNameTest() {
		List<DefaultSettings> settingsList=Settings.getDefaultSettingsByPrimIdAndType(settings.getPrimId(), settings.getType());
		
		Assert.assertEquals(settingsList.size(),1);
		Assert.assertTrue(settingsList.get(0).equals(settings));
	}
	
	
	@StarexecTest
	private void getSettingsTest() {
		try {
			DefaultSettings temp = Settings.getProfileById(settings.getId());
			Assert.assertNotNull(temp);
			Assert.assertTrue(temp.equals(settings));
		} catch (SQLException e) {
			Assert.fail("Caught SQLException: " + Util.getStackTrace(e));
		}
	}
	
	@StarexecTest
	private void updateDefaultMemoryLimitTest() {

		
		Assert.assertTrue(Settings.setDefaultMaxMemory(settings.getId(), settings.getMaxMemory()+1));
		try {
			Assert.assertEquals(settings.getMaxMemory() + 1, Settings.getProfileById(settings.getId()).getMaxMemory());
			Assert.assertTrue(Settings.setDefaultMaxMemory(settings.getId(), settings.getMaxMemory()));
		} catch (SQLException e) {
			Assert.fail("Caught SQLException: " + Util.getStackTrace(e));
		}
	}
	
	@Override
	protected void setup() throws Exception {
		u=loader.loadUserIntoDatabase();
		u2=loader.loadUserIntoDatabase();

		space=loader.loadSpaceIntoDatabase(u.getId(), Communities.getTestCommunity().getId());

		benchIds = loader.loadBenchmarksIntoDatabase(space.getId() ,u.getId());
		
		settings=loader.loadDefaultSettingsProfileIntoDatabase(u.getId());
		settings2=loader.loadDefaultSettingsProfileIntoDatabase(u2.getId());
		admin=loader.loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa",R.ADMIN_ROLE_NAME);
	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}

}
