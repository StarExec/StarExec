package org.starexec.test.integration.database;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Settings;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.DefaultSettings;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.util.Util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for org.starexec.data.database.Settings.java
 * @author Eric
 */
public class DefaultSettingsTests extends TestSequence {
	DefaultSettings settings=null;
	DefaultSettings settings2=null;
    DefaultSettings settingsWithDefaultBenchmarks=null;
	User u=null;
	User u2=null;
    User u3=null;

	List<Integer> benchIds;

	Space space = null;

	User admin=null;
	@Override
	protected String getTestName() {
		return "DefaultSettingsTests";
	}

	@StarexecTest
    private void addAndDeleteDefaultBenchmarkTest() {
        try {
            int benchId = benchIds.get(0);
            int settingId = settings.getId();
            DefaultSettings s = Settings.getProfileById(settingId);

            Assert.assertFalse("Benchmark was already in settings.", s.getBenchIds().contains(benchId));

            Settings.addDefaultBenchmark(settingId, benchId);

            s = Settings.getProfileById(settingId);

            Assert.assertTrue("Default benchmark was not added.", s.getBenchIds().contains(benchId));

            Settings.deleteDefaultBenchmark(settingId, benchId);

            s = Settings.getProfileById(settingId);

            Assert.assertFalse("Benchmark not deleted from settings.", s.getBenchIds().contains(benchId));

        } catch (SQLException e) {
            Assert.fail("Caught SQLException: " + Util.getStackTrace(e));
        }
    }

	@StarexecTest
	private void addSettingsTest() {
		DefaultSettings newSettings = DefaultSettings.copy(settings);

        newSettings.setBenchIds(benchIds);

		int id = Settings.addNewSettingsProfile(newSettings);

        Assert.assertNotEquals("Error while adding new settings.", id, -1);

        try {
            DefaultSettings dbSettings = Settings.getProfileById(newSettings.getId());

            assertDefaultSettingsEqual(newSettings, dbSettings);
        } catch(SQLException e) {
            Assert.fail("Caught SQLException: " + Util.getStackTrace(e));
        } finally {
            Settings.deleteProfile(newSettings.getId());
        }

	}

	@StarexecTest
    private void getDefaultBenchmarksTest() {
        int settingsId = settingsWithDefaultBenchmarks.getId();
        try {
            List<Benchmark> dbBenchmarks = Settings.getDefaultBenchmarks(settingsId);
            Collections.sort(dbBenchmarks, (a,b) -> ((Integer)a.getId()).compareTo(b.getId()));
            List<Integer> settingsBenchmarks = settingsWithDefaultBenchmarks.getBenchIds();
            Collections.sort(settingsBenchmarks);

            Assert.assertTrue(settingsBenchmarks.size() == dbBenchmarks.size());

            for (int i = 0; i < settingsBenchmarks.size(); i++) {
                Assert.assertEquals(settingsBenchmarks.get(i), (Integer)dbBenchmarks.get(i).getId());
            }
        } catch (SQLException e) {
            Assert.fail("SQLException thrown: " + Util.getStackTrace(e));
        }
    }

    @StarexecTest
    private void getDefaultBenchmarkIdsTest() {
        int settingsId = settingsWithDefaultBenchmarks.getId();
        try {
            List<Integer> dbBenchmarks = Settings.getDefaultBenchmarkIds(settingsId);
            Collections.sort(dbBenchmarks);
            List<Integer> settingsBenchmarks = settingsWithDefaultBenchmarks.getBenchIds();
            Collections.sort(settingsBenchmarks);

            Assert.assertTrue(settingsBenchmarks.size() == dbBenchmarks.size());

            for (int i = 0; i < settingsBenchmarks.size(); i++) {
                Assert.assertEquals(settingsBenchmarks.get(i), dbBenchmarks.get(i));
            }
        } catch (SQLException e) {
            Assert.fail("SQLException thrown: " + Util.getStackTrace(e));
        }
    }

    @StarexecTest
    private void updateDefaultSettingsTest() {
        DefaultSettings newSettings = DefaultSettings.copy(settingsWithDefaultBenchmarks);
        Settings.addNewSettingsProfile(newSettings);

        newSettings.setMaxMemory(newSettings.getMaxMemory()+1);
        newSettings.setCpuTimeout(newSettings.getCpuTimeout()+1);
        newSettings.setWallclockTimeout(newSettings.getWallclockTimeout()+1);
        newSettings.setDependenciesEnabled(!newSettings.isDependenciesEnabled());
        newSettings.setBenchIds(new ArrayList<>(Arrays.asList(
                new Integer[] {newSettings.getBenchIds().get(0)}
        )));

        // TODO: need to change all other fields of setting except name, primId, and type (these are immutable)

        boolean success = Settings.updateDefaultSettings(newSettings);
        Assert.assertTrue("Database call failed.", success);
        try {
            DefaultSettings dbSettings = Settings.getProfileById(newSettings.getId());
            assertDefaultSettingsEqual(newSettings, dbSettings);
        } catch (SQLException e) {
            Assert.fail("Caught SQLException: " + Util.getStackTrace(e));
        } finally {
            Settings.deleteProfile(newSettings.getId());
        }
    }

    private void assertDefaultSettingsEqual(DefaultSettings newSettings, DefaultSettings dbSettings) {

        Assert.assertNotNull("The new setting object was null.", newSettings);
        Assert.assertNotNull("The DB setting object was null.", dbSettings);


        Assert.assertEquals("PrimId was not equal.", newSettings.getPrimId(), dbSettings.getPrimId());
        Assert.assertEquals("PreProcessorId was not equal.", newSettings.getPreProcessorId(), dbSettings.getPreProcessorId());
        Assert.assertEquals("PostProcessorId was not equal.", newSettings.getPostProcessorId(), dbSettings.getPostProcessorId());
        Assert.assertEquals("BenchProcessorId was not equal.", newSettings.getBenchProcessorId(), dbSettings.getBenchProcessorId());
        Assert.assertEquals("SolverId was not equal.", newSettings.getSolverId(), dbSettings.getSolverId());
        Assert.assertEquals("WallclockTimeout was not equal.", newSettings.getWallclockTimeout(), dbSettings.getWallclockTimeout());
        Assert.assertEquals("CpuTimeout was not equal.", newSettings.getCpuTimeout(), dbSettings.getCpuTimeout());
        Assert.assertEquals("MaxMemory was not equal.", newSettings.getMaxMemory(), dbSettings.getMaxMemory());
        Assert.assertEquals("DependenciesEnabled was not equal.", newSettings.isDependenciesEnabled(), dbSettings.isDependenciesEnabled());
        Assert.assertEquals("Name was not equal.", newSettings.getName(), dbSettings.getName());
        Assert.assertEquals("Type was not equal.", newSettings.getType(), dbSettings.getType());
        Assert.assertEquals("Benchmarking framework was not equal.", newSettings.getBenchmarkingFramework(), dbSettings.getBenchmarkingFramework());

        List<Integer> settingsBenchIds = newSettings.getBenchIds();
        List<Integer> dbSettingsBenchIds = dbSettings.getBenchIds();

        final String benchIdsNotEqual = "Bench ids were not equal.";
        Assert.assertEquals(benchIdsNotEqual, settingsBenchIds.size(), dbSettingsBenchIds.size());

        Collections.sort(settingsBenchIds);
        Collections.sort(dbSettingsBenchIds);
        for (int i = 0; i < settingsBenchIds.size(); i++) {
            Assert.assertEquals(benchIdsNotEqual, settingsBenchIds.get(i), dbSettingsBenchIds.get(i));
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
        u3=loader.loadUserIntoDatabase();

		space=loader.loadSpaceIntoDatabase(u.getId(), Communities.getTestCommunity().getId());

		benchIds = loader.loadBenchmarksIntoDatabase(space.getId() ,u.getId());

		settings=loader.loadDefaultSettingsProfileIntoDatabase(u.getId());
		settings2=loader.loadDefaultSettingsProfileIntoDatabase(u2.getId());
        settingsWithDefaultBenchmarks=loader.loadDefaultSettingsProfileIntoDatabaseWithDefaultBenchmarks(u3.getId(), benchIds);

		admin=loader.loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa",R.ADMIN_ROLE_NAME);
	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}

}
