package org.starexec.test.integration.database;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import org.junit.Assert;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.data.to.Job;
import org.starexec.exceptions.StarExecSecurityException;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;

/**
 * Tests for org.starexec.data.database.Solvers.java
 * @author Eric
 */
public class SolverTests extends TestSequence {
	private Solver solver; // placed in space1
	private Configuration config;
	private Space space1, space2, testCommunity; // spaces owned by testUser. Space2 a subspace of space1
	User testUser;
	List<Integer> benchmarkIds = null; 
	Job job = null; // uses solver and benchmarks
	
	@StarexecTest
	private void GetSolverTest() {
		Solver cs=Solvers.get(solver.getId());
		Assert.assertNotNull(cs);
		Assert.assertEquals(cs.getId(),solver.getId());
	}
	@StarexecTest
	private void GetSolverListTest() {
		List<Integer> list=new ArrayList<Integer>();
		list.add(solver.getId());
		List<Solver> cs=Solvers.get(list);
		Assert.assertEquals(1,cs.size());
		Assert.assertEquals(solver.getId(), cs.get(0).getId());
	}

	@StarexecTest
	private void getSolversInSpacesAndJobsContainsConfigs() {
		Set<Integer> spaces = new HashSet<>();
		spaces.add(space1.getId());
		try {
			List<Solver> solvers = Solvers.getSolversInSpacesAndJob(job.getId(), spaces);
			for (Solver s : solvers) {
				Assert.assertTrue(
						"Solver had no configurations. This method must return detailed solvers.",
						s.getConfigurations().size() > 0);
			}
		} catch (SQLException e) {
			Assert.fail("Caught SQLException: "+ Util.getStackTrace(e));
		}
	}
	
	@StarexecTest 
	private void recycleAndRestoreTest() {
		Assert.assertFalse(Solvers.isSolverRecycled(solver.getId()));
		Assert.assertTrue(Solvers.recycle(solver.getId()));
		Assert.assertTrue(Solvers.isSolverRecycled(solver.getId()));
		Assert.assertTrue(Solvers.restore(solver.getId()));
		Assert.assertFalse(Solvers.isSolverRecycled(solver.getId()));
	}
	
	@StarexecTest
	private void changeNameTest() {
		String curName=solver.getName();
		Solver cs=Solvers.get(solver.getId());
		Assert.assertEquals(curName, cs.getName());
		String newName=TestUtil.getRandomSolverName();
		Assert.assertTrue(Solvers.updateDetails(solver.getId(),newName,solver.getDescription(),solver.isDownloadable()));
		cs=Solvers.get(solver.getId());
		Assert.assertEquals(newName, cs.getName());
		solver.setName(newName);
	}
	
	@StarexecTest
	private void getConfigTest() {
		Configuration c=Solvers.getConfiguration(config.getId());
		Assert.assertNotNull(c);
		Assert.assertEquals(config.getName(),c.getName());
	}
	
	@StarexecTest
	private void getConfigsBySolver() {
		List<Configuration> configs=Solvers.getConfigsForSolver(solver.getId());
		Assert.assertNotNull(configs);
		Assert.assertTrue(configs.size()>=1);
		boolean foundConfig=false;
		for (Configuration c : configs) {
			if (c.getId()==config.getId()) {
				foundConfig=true;
				break;
			}
		}
		Assert.assertTrue(foundConfig);
	}
	
	@StarexecTest
	private void getWithConfig() {
		Solver s=Solvers.getWithConfig(solver.getId(), config.getId());
		Assert.assertNotNull(s);
		Assert.assertEquals(1,s.getConfigurations().size());
		Configuration c=s.getConfigurations().get(0);
		Assert.assertEquals(config.getName(),c.getName());
		
	}
	
	@StarexecTest 
	private void updateConfigName() {
		String oldName=config.getName();
		Assert.assertEquals(Solvers.getConfiguration(config.getId()).getName(),oldName);
		String newName=TestUtil.getRandomSolverName();
		Assert.assertTrue(Solvers.updateConfigDetails(config.getId(), newName, ""));
		Assert.assertEquals(Solvers.getConfiguration(config.getId()).getName(),newName);
		config.setName(newName);
	}
	
	@StarexecTest
	private void updateConfigDescription() {
		String oldDesc=config.getDescription();
		Assert.assertEquals(Solvers.getConfiguration(config.getId()).getDescription(),oldDesc);
		String newDesc=TestUtil.getRandomSolverName();
		Assert.assertTrue(Solvers.updateConfigDetails(config.getId(), config.getName(), newDesc));
		Assert.assertEquals(Solvers.getConfiguration(config.getId()).getDescription(),newDesc);
		config.setDescription(newDesc);
	}
	@StarexecTest 
	private void deleteConfigTest() {
		Configuration c=loader.loadConfigurationFileIntoDatabase("CVC4Config.txt", solver.getId());
		Assert.assertNotNull(c);
		Assert.assertNotNull(Solvers.getConfiguration(c.getId()));
		Assert.assertTrue(Solvers.deleteConfiguration(c.getId()));
		Assert.assertNull(Solvers.getConfiguration(c.getId()));
	}
	
	@StarexecTest
	private void changeDescTest() {
		String curDesc=solver.getDescription();
		Solver cs=Solvers.get(solver.getId());
		Assert.assertEquals(curDesc, cs.getDescription());
		String newDesc=TestUtil.getRandomSolverName();
		Assert.assertTrue(Solvers.updateDetails(solver.getId(),solver.getName(),newDesc,solver.isDownloadable()));
		cs=Solvers.get(solver.getId());
		Assert.assertEquals(newDesc, cs.getDescription());
		solver.setDescription(newDesc);
	}
	
	@StarexecTest
	private void changeDownloadableTest() {
		Solver cs=Solvers.get(solver.getId());
		Assert.assertEquals(solver.isDownloadable(), cs.isDownloadable());
		Assert.assertTrue(Solvers.updateDetails(solver.getId(),solver.getName(),solver.getDescription(),!solver.isDownloadable()));
		cs=Solvers.get(solver.getId());
		Assert.assertNotEquals(solver.isDownloadable(), cs.isDownloadable());
		solver.setDownloadable(!solver.isDownloadable());
	}
	
	@StarexecTest
	private void associateTest() {
		Assert.assertTrue(Solvers.associate(solver.getId(), space2.getId()));
		boolean found = false;
		for (Solver sol : Spaces.getDetails(space2.getId(), testUser.getId()).getSolvers()) {
			found = found || sol.getId()==solver.getId();
		}
		Assert.assertTrue(found);
		List<Integer> ids = new ArrayList<Integer>();
		ids.add(solver.getId());
		Spaces.removeSolvers(ids, space2.getId());
	}
	
	@StarexecTest
	private void associateInHierarchyTest() {
		List<Integer> ids = new ArrayList<Integer>();
		ids.add(solver.getId());
		
		Assert.assertTrue(Solvers.associate(ids,space1.getId(),true,testUser.getId(),false));
		boolean found = false;
		for (Solver sol : Spaces.getDetails(space2.getId(), testUser.getId()).getSolvers()) {
			found = found || sol.getId()==solver.getId();
		}
		Assert.assertTrue(found);
		
		Spaces.removeSolvers(ids, space2.getId());
	}
	
	@StarexecTest
	private void copySolverTest() {
		int id = Solvers.copySolver(Solvers.get(solver.getId()), testUser.getId(), space2.getId());
		Assert.assertTrue(id>0);
		Solver newSolver = Solvers.get(id);
		Assert.assertNotEquals(newSolver.getId(), solver.getId());
		Assert.assertEquals(solver.getName(), newSolver.getName());
		Assert.assertTrue(new File(newSolver.getPath()).exists());
		Solvers.deleteAndRemoveSolver(newSolver.getId());
	}
	
	@StarexecTest
	private void copySolversTest() {
		List<Solver> sols = new ArrayList<Solver>();
		sols.add(Solvers.get(solver.getId()));
		List<Integer> ids = Solvers.copySolvers(sols, testUser.getId(), space2.getId());
		Assert.assertEquals(1, ids.size());
		int id = ids.get(0);
		Assert.assertTrue(id>0);
		Solver newSolver = Solvers.get(id);
		Assert.assertNotEquals(newSolver.getId(), solver.getId());
		Assert.assertEquals(solver.getName(), newSolver.getName());
		Assert.assertTrue(new File(newSolver.getPath()).exists());
		Solvers.deleteAndRemoveSolver(newSolver.getId());
	}
	
	@StarexecTest
	private void cleanOrphanedDeletedSolversTest() {
		Solver s = loader.loadSolverIntoDatabase(space1.getId(), testUser.getId());
		Solvers.delete(s.getId());
		Assert.assertTrue(Solvers.cleanOrphanedDeletedSolvers());
		Assert.assertNotNull(Solvers.getIncludeDeleted(s.getId()));
		List<Integer> ids = new ArrayList<Integer>();
		ids.add(s.getId());
		Spaces.removeSolvers(ids, space1.getId());
		Assert.assertTrue(Solvers.cleanOrphanedDeletedSolvers());
		Assert.assertNull(Solvers.getIncludeDeleted(s.getId()));
	}
	
	@StarexecTest
	private void deleteSolverTest() {
		Solver s = loader.loadSolverIntoDatabase(space1.getId(), testUser.getId());
		Assert.assertTrue(Solvers.delete(s.getId()));
		s = Solvers.getIncludeDeleted(s.getId());
		Assert.assertTrue(s.isDeleted());
		Assert.assertFalse(new File(s.getPath()).exists());
		Solvers.deleteAndRemoveSolver(s.getId());
	}
	
	@StarexecTest
	private void findConfigsEmptyDirTest() {
		Assert.assertEquals(0, Solvers.findConfigs("/starexec/fake/test/directory").size());
	}
	
	@StarexecTest
	private void findConfigsTest() throws IOException {
		List<String> strs = loader.getTestConfigDirectory();
		List<Configuration> configurations = Solvers.findConfigs(strs.get(0));
		Assert.assertEquals(2, configurations.size());
		Assert.assertTrue(strs.contains(configurations.get(0).getName()));
		Assert.assertTrue(strs.contains(configurations.get(1).getName()));
	}
	
	@StarexecTest
	private void getAssociatedSpaceIdsTest() {
		List<Integer> ids = Solvers.getAssociatedSpaceIds(solver.getId());
		Assert.assertEquals(1, ids.size());
		Assert.assertEquals((Integer)space1.getId(), ids.get(0));
	}
	
	@StarexecTest
	private void getByConfigIdTest() {
		Assert.assertEquals(solver.getId(), Solvers.getByConfigId(config.getId()).getId());
	}
	
	@StarexecTest
	private void getSolversInSharedSpacesTest() {
		List<Solver> solvers = Solvers.getSolversInSharedSpaces(testUser.getId());
		Assert.assertEquals(1, solvers.size());
		Assert.assertEquals(solver.getId(), solvers.get(0).getId());
	}
	
	@StarexecTest
	private void getBySpaceTest() {
		List<Solver> solvers = Solvers.getBySpace(space1.getId());
		Assert.assertEquals(1, solvers.size());
		Assert.assertEquals(solver.getId(), solvers.get(0).getId());
	}
	
	@StarexecTest
	private void getBySpaceDetailedTest() {
		List<Solver> solvers = Solvers.getBySpaceDetailed(space1.getId());
		Assert.assertEquals(1, solvers.size());
		Assert.assertEquals(solver.getId(), solvers.get(0).getId());
		Assert.assertEquals(solver.getConfigurations().size(), solvers.get(0).getConfigurations().size());
	}
	
	@StarexecTest
	private void getBySpaceHierarchyTest() {
		Solver newSolver = loader.loadSolverIntoDatabase(space2.getId(), testUser.getId());
		Solvers.associate(newSolver.getId(), space2.getId());
		List<Solver> solvers = Solvers.getBySpaceHierarchy(space1.getId(), testUser.getId());
		Assert.assertEquals(2, solvers.size());
		for (Solver s : solvers) {
			Assert.assertTrue(s.getId()==newSolver.getId() || s.getId()==solver.getId());
		}
		
		Solvers.deleteAndRemoveSolver(newSolver.getId());
	}
	
	@StarexecTest
	private void getByOwnerTest() {
		List<Solver> solvers = Solvers.getByOwner(testUser.getId());
		Assert.assertEquals(1, solvers.size());
		Assert.assertEquals(solver.getId(), solvers.get(0).getId());
	}
	
	@StarexecTest
	private void getByUserTest() {
		List<Solver> solvers = Solvers.getByUser(testUser.getId());
		boolean found = false;
		for (Solver s : solvers) {
			found = found || s.getId()==solver.getId();
			Assert.assertTrue(Solvers.isPublic(s.getId()) || s.getId()==solver.getId());
		}
		Assert.assertTrue(found);
	}
	
	@StarexecTest
	private void getByJobSimpleWithConfigsTest() throws SQLException {
		List<Solver> solvers = Solvers.getByJobSimpleWithConfigs(job.getId());
		Assert.assertEquals(1, solvers.size());
		Assert.assertEquals(solver.getId(), solvers.get(0).getId());
	}
	
	@StarexecTest
	private void getConfigIdSetByJobTest() throws SQLException {
		Set<Integer> configIds = Solvers.getConfigIdSetByJob(job.getId());
		Assert.assertEquals(1, configIds.size());
		Assert.assertEquals((Integer)config.getId(), configIds.iterator().next());
	}
	
	@StarexecTest
	private void getCountInSpaceTest() {
		Assert.assertEquals(1, Solvers.getCountInSpace(space1.getId()));
	}
	
	@StarexecTest
	private void getCountInSpaceQueryTest() {
		Assert.assertEquals(1, Solvers.getCountInSpace(space1.getId(), ""));
		Assert.assertEquals(1, Solvers.getCountInSpace(space1.getId(), solver.getName()));
		Assert.assertEquals(0, Solvers.getCountInSpace(space1.getId(), TestUtil.getRandomAlphaString(100)));
	}
	
	@StarexecTest
	private void getPublicSolversTest() {
		boolean found = false;
		boolean isPublic = Spaces.isPublicSpace(space1.getId());
		for (Solver s : Solvers.getPublicSolvers()) {
			found = found || s.getId()==solver.getId();
		}
		Assert.assertEquals(isPublic, found);
		found = false;
		Spaces.setPublicSpace(space1.getId(), testUser.getId(), !isPublic, false);
		for (Solver s : Solvers.getPublicSolvers()) {
			found = found || s.getId()==solver.getId();
		}
		Assert.assertNotEquals(isPublic, found);
		Spaces.setPublicSpace(space1.getId(), testUser.getId(), isPublic, false);
		
	}
	
	@Override
	protected void setup() throws Exception {
		testUser=loader.loadUserIntoDatabase();
		testCommunity=Communities.getTestCommunity();
		space1=loader.loadSpaceIntoDatabase(testUser.getId(),testCommunity.getId());
		space2=loader.loadSpaceIntoDatabase(testUser.getId(), space1.getId());
		solver=loader.loadSolverIntoDatabase("CVC4.zip", space1.getId(), testUser.getId());
		config=solver.getConfigurations().get(0);
		benchmarkIds = loader.loadBenchmarksIntoDatabase(space1.getId(), testUser.getId());
		job = loader.loadJobIntoDatabase(space1.getId(), testUser.getId(), solver.getId(), benchmarkIds);
	}

	@Override
	protected void teardown() throws StarExecSecurityException {
		loader.deleteAllPrimitives();
	}
		
	
	@Override
	protected String getTestName() {
		return "SolverTestSequence";
	}

}
