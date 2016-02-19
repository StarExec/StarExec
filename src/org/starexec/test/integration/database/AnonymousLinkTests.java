
package org.starexec.test.integration.database;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.junit.Assert;

import org.starexec.constants.R;
import org.starexec.data.database.AnonymousLinks;
import org.starexec.data.database.AnonymousLinks.PrimitivesToAnonymize;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Job;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.LogUtil;
import org.starexec.util.Util;

public class AnonymousLinkTests extends TestSequence {
	private static final Logger log = Logger.getLogger(AnonymousLinkTests.class);	
	private static final LogUtil logUtil = new LogUtil( log );
	private User admin;
	private User user;
	private Space space;
	private Job job=null;       
	private List<Benchmark> benchmarks = null;
	private Solver solver = null;
	// Create a list of all PrimitivesToAnonymize enums. (Need a list rather than a set to guarantee iteration order )
	private List<PrimitivesToAnonymize> primitivesToAnonymizeList = new ArrayList<>( EnumSet.allOf( PrimitivesToAnonymize.class ) );


	@StarexecTest
	private void addAnonymousLinkTest() {
		final String methodName = "AddAnonymousLinkTest";
		List<String> newUuids = new ArrayList<>();
		try {
			for ( PrimitivesToAnonymize primitivesToAnonymize : primitivesToAnonymizeList ) {
				for ( Benchmark bench : benchmarks ) {
					String uuid = AnonymousLinks.addAnonymousLink( R.BENCHMARK, bench.getId(), primitivesToAnonymize );
					newUuids.add( uuid );
				}
			}

			for ( PrimitivesToAnonymize primitivesToAnonymize : primitivesToAnonymizeList ) {
				String uuid = AnonymousLinks.addAnonymousLink( R.SOLVER, solver.getId(), primitivesToAnonymize );
				newUuids.add( uuid );
			}

			for ( PrimitivesToAnonymize primitivesToAnonymize : primitivesToAnonymizeList ) {
				String uuid = AnonymousLinks.addAnonymousLink( R.JOB, job.getId(), primitivesToAnonymize );
				newUuids.add( uuid );
			}

			// Cleanup the anonymous links.
			AnonymousLinks.delete( newUuids );

			Set<String> newUuidsSet = new HashSet<>( newUuids );

			// Assert that there are no duplicate entries in newUuids
			Assert.assertEquals( "Duplicate entries in newUuids.", newUuidsSet.size(), newUuids.size() );

		} catch ( SQLException e ) {
			logUtil.error( methodName, "An SQLException was thrown during the test: " + Util.getStackTrace(e) );
			Assert.fail( "An SQLException was thrown during the test: " + Util.getStackTrace(e) );
		}
	}

	@StarexecTest
	private void getAnonymousLinkCodeTest() {
		final String methodName = "GetAnonymousLinkCodeTest";
		List<String> newUuids = new ArrayList<>();
		try {
			for ( PrimitivesToAnonymize primitivesToAnonymize : primitivesToAnonymizeList ) {
				for ( Benchmark bench : benchmarks ) {
					String uuid = AnonymousLinks.addAnonymousLink( R.BENCHMARK, bench.getId(), primitivesToAnonymize );
					Optional<String> getUuid= AnonymousLinks.getAnonymousLinkCode( R.BENCHMARK, bench.getId(), primitivesToAnonymize );

					Assert.assertTrue( getUuid.isPresent() );

					Assert.assertEquals( 
							getAnonymousLinkCodeTestMessage(uuid, getUuid.get(), R.BENCHMARK, bench.getId(), primitivesToAnonymize ),
							uuid,
							getUuid.get());

					newUuids.add( uuid );
				}
			}

			for ( PrimitivesToAnonymize primitivesToAnonymize : primitivesToAnonymizeList ) {
				String uuid = AnonymousLinks.addAnonymousLink( R.SOLVER, solver.getId(), primitivesToAnonymize );
				Optional<String> getUuid= AnonymousLinks.getAnonymousLinkCode( R.SOLVER, solver.getId(), primitivesToAnonymize );

				Assert.assertTrue( getUuid.isPresent() );

				Assert.assertEquals( 
						getAnonymousLinkCodeTestMessage(uuid, getUuid.get(), R.SOLVER, solver.getId(), primitivesToAnonymize ),
						uuid,
						getUuid.get());

				newUuids.add( uuid );
			}

			for ( PrimitivesToAnonymize primitivesToAnonymize : primitivesToAnonymizeList ) {
				String uuid = AnonymousLinks.addAnonymousLink( R.JOB, job.getId(), primitivesToAnonymize );

				Optional<String> getUuid = AnonymousLinks.getAnonymousLinkCode( R.JOB, job.getId(), primitivesToAnonymize );
				Assert.assertTrue( getUuid.isPresent() );

				Assert.assertEquals( 
						getAnonymousLinkCodeTestMessage(uuid, getUuid.get(), R.JOB, job.getId(), primitivesToAnonymize ),
						uuid,
						getUuid.get());

				newUuids.add( uuid );
			}

			// Cleanup the anonymous links.
			AnonymousLinks.delete( newUuids );
		} catch ( SQLException e ) {
			logUtil.error( methodName, "An SQLException was thrown during the test: " + Util.getStackTrace(e) );
			Assert.fail( "An SQLException was thrown during the test: " + Util.getStackTrace(e) );
		}
	}

	private String getAnonymousLinkCodeTestMessage( 
			String uuid, 
			String getUuid, 
			String primitiveType, 
			int primitiveId, 
			PrimitivesToAnonymize primitivesToAnonymize) {
			return uuid + " != " + getUuid + " for primType=" + R.JOB +",  primId="+job.getId()+", primitivesToAnonymize=" +
					AnonymousLinks.getPrimitivesToAnonymizeName(primitivesToAnonymize);
	}

	@StarexecTest
	private void getIdOfJobAssociatedWithLinkTest() {
		final String methodName = "GetIdOfJobAssociatedWithLinkTest";
		getIdOfPrimitiveAssociatedWithLinkHelper( R.JOB, job.getId(), methodName ); 
	}

	@StarexecTest
	private void getIdOfBenchmarkAssociatedWithLinkTest() {
		final String methodName = "GetIdOfBenchmarkAssociatedWithLinkTest";
		for ( Benchmark bench : benchmarks ) {
			getIdOfPrimitiveAssociatedWithLinkHelper( R.BENCHMARK, bench.getId(), methodName ); 
		}
	}

	@StarexecTest
	private void getIdOfSolverAssociatedWithLinkTest() {
		final String methodName = "GetIdOfSolverAssociatedWithLinkTest";
		getIdOfPrimitiveAssociatedWithLinkHelper( R.SOLVER, solver.getId(), methodName ); 
	}

	private void getIdOfPrimitiveAssociatedWithLinkHelper(
			final String primitiveName, 
			final int primitiveId, 
			final String methodName) {
		List<String> newUuids = new ArrayList<>();
		try {
			for ( PrimitivesToAnonymize primitivesToAnonymize : primitivesToAnonymizeList ) {
				String uuid = AnonymousLinks.addAnonymousLink( primitiveName, primitiveId, primitivesToAnonymize );

				Optional<Integer> primitiveIdFromDb = Optional.empty();

				if ( primitiveName.equals( R.JOB ) ) {
					primitiveIdFromDb = AnonymousLinks.getIdOfJobAssociatedWithLink( uuid );
				} else if ( primitiveName.equals( R.SOLVER ) ) {
					primitiveIdFromDb = AnonymousLinks.getIdOfSolverAssociatedWithLink( uuid );
				} else if ( primitiveName.equals( R.BENCHMARK ) ) {
					primitiveIdFromDb = AnonymousLinks.getIdOfBenchmarkAssociatedWithLink( uuid );
				} else {
					Assert.fail("Invalid primitive name given: " + primitiveName );
				}

				Assert.assertTrue( primitiveIdFromDb.isPresent() );
				Assert.assertEquals( 
					"The id retrieved from the database was not the same as the one given to the database for "
							+ AnonymousLinks.getPrimitivesToAnonymizeName( primitivesToAnonymize),
					(int)primitiveIdFromDb.get(), 
					(int)primitiveId );

				newUuids.add( uuid );
			}

			// Cleanup the anonymous links.
			AnonymousLinks.delete( newUuids );

		} catch ( SQLException e ) {
			logUtil.error( methodName, "An SQLException was thrown during the test: " + Util.getStackTrace(e) );
			Assert.fail( "An SQLException was thrown during the test: " + Util.getStackTrace(e) );
		}
	}

	@StarexecTest
	private void getPrimitivesToAnonymizeForSolvertTest() {
		final String methodName = "getPrimitivesToAnonymizeForSolvertTest";
		getPrimitivesToAnonymizeHelper( R.SOLVER, solver.getId(), methodName );
	}
	@StarexecTest
	private void getPrimitivesToAnonymizeForBenchmarkTest() {
		final String methodName = "getPrimitivesToAnonymizeForSolvertTest";
		for ( Benchmark bench : benchmarks ) {
			getPrimitivesToAnonymizeHelper( R.BENCHMARK, bench.getId(), methodName );
		}
	}
	@StarexecTest
	private void getPrimitivesToAnonymizeForJobTest() {
		final String methodName = "getPrimitivesToAnonymizeForSolvertTest";
		getPrimitivesToAnonymizeHelper( R.JOB, job.getId(), methodName );
	}

	private void getPrimitivesToAnonymizeHelper(
			final String primitiveName, 
			final int primitiveId,
			final String methodName) {
		List<String> newUuids = new ArrayList<>();
		try {
			for ( final PrimitivesToAnonymize primitivesToAnonymize : primitivesToAnonymizeList ) {
				final String uuid = AnonymousLinks.addAnonymousLink( primitiveName, primitiveId, primitivesToAnonymize );

				Optional<PrimitivesToAnonymize> primitivesToAnonymizeFromDb = Optional.empty();

				if ( primitiveName.equals( R.JOB ) ) {
					primitivesToAnonymizeFromDb = AnonymousLinks.getPrimitivesToAnonymizeForJob( uuid );
				} else if ( primitiveName.equals( R.SOLVER ) ) {
					primitivesToAnonymizeFromDb = AnonymousLinks.getPrimitivesToAnonymizeForSolver( uuid );
				} else if ( primitiveName.equals( R.BENCHMARK ) ) {
					primitivesToAnonymizeFromDb = AnonymousLinks.getPrimitivesToAnonymizeForBenchmark( uuid );
				} else {
					Assert.fail("Invalid primitive name given: " + primitiveName );
				}

				Assert.assertTrue( primitivesToAnonymizeFromDb.isPresent() );
				Assert.assertEquals( 
						"The PrimitivesToAnonymize retrieved from the database was not the same as the one given to the database.", 
						primitivesToAnonymize,
						primitivesToAnonymizeFromDb.get());

				newUuids.add( uuid );
			}

			// Cleanup the anonymous links.
			AnonymousLinks.delete( newUuids );

		} catch ( SQLException e ) {
			logUtil.error( methodName, "An SQLException was thrown during the test: " + Util.getStackTrace(e) );
			Assert.fail( "An SQLException was thrown during the test: " + Util.getStackTrace(e) );
		}
	}

	@StarexecTest
	public void deleteTest() {
		final String methodName = "deleteTest";
		try {
			List<String> newUuids = new ArrayList<>();

			for ( PrimitivesToAnonymize primitivesToAnonymize : primitivesToAnonymizeList ) {
				for ( Benchmark bench : benchmarks ) {
					String uuid = AnonymousLinks.addAnonymousLink( R.BENCHMARK, bench.getId(), primitivesToAnonymize );
					newUuids.add( uuid );
				}
			}

			// Delete the links.
			AnonymousLinks.delete( newUuids );

			for ( String uuid : newUuids ) {
				// Now calls to the DB should return empty Optionals
				Optional<Integer> potentialId = AnonymousLinks.getIdOfBenchmarkAssociatedWithLink( uuid );
				Assert.assertFalse( potentialId.isPresent() );
				Optional<PrimitivesToAnonymize> potentialPta = AnonymousLinks.getPrimitivesToAnonymizeForBenchmark( uuid );
				Assert.assertFalse( potentialPta.isPresent() );
			}


			// Reset the uuid list. 
			newUuids = new ArrayList<>();

			for ( PrimitivesToAnonymize primitivesToAnonymize : primitivesToAnonymizeList ) {
				String uuid = AnonymousLinks.addAnonymousLink( R.SOLVER, solver.getId(), primitivesToAnonymize );
				newUuids.add( uuid );
			}

			// Delete the links.
			AnonymousLinks.delete( newUuids );

			for ( String uuid : newUuids ) {
				// Now calls to the DB should return empty Optionals
				Optional<Integer> potentialId = AnonymousLinks.getIdOfSolverAssociatedWithLink( uuid );
				Assert.assertFalse( potentialId.isPresent() );
				Optional<PrimitivesToAnonymize> potentialPta = AnonymousLinks.getPrimitivesToAnonymizeForSolver( uuid );
				Assert.assertFalse( potentialPta.isPresent() );
			}

			newUuids = new ArrayList<>();
			for ( PrimitivesToAnonymize primitivesToAnonymize : primitivesToAnonymizeList ) {
				String uuid = AnonymousLinks.addAnonymousLink( R.JOB, job.getId(), primitivesToAnonymize );
				newUuids.add( uuid );
			}

			// Delete the links.
			AnonymousLinks.delete( newUuids );

			for ( String uuid : newUuids ) {
				// Now calls to the DB should return empty Optionals
				Optional<Integer> potentialId = AnonymousLinks.getIdOfJobAssociatedWithLink( uuid );
				Assert.assertFalse( potentialId.isPresent() );
				Optional<PrimitivesToAnonymize> potentialPta = AnonymousLinks.getPrimitivesToAnonymizeForJob( uuid );
				Assert.assertFalse( potentialPta.isPresent() );
			}

		} catch ( SQLException e ) {
			logUtil.error( methodName, "An SQLException was thrown during the test: " + Util.getStackTrace(e) );
			Assert.fail( "An SQLException was thrown during the test: " + Util.getStackTrace(e) );
		}
		
	}	


	@Override
	protected String getTestName() {
		return "AnonymousLinkTests";
	}

	@Override
	protected void setup() {

		// Get admin user.
		admin = Users.getAdmins().get(0);

		// Setup test user.
		user=Users.getTestUser();

		// Setup test space.
		Space community = Communities.getTestCommunity();
		space = ResourceLoader.loadSpaceIntoDatabase( user.getId(), community.getId() );

		// Setup test benchmarks
		List<Integer> benchmarkIds = ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip", space.getId(), user.getId());
		benchmarks = new ArrayList<Benchmark>();
		for ( Integer id : benchmarkIds ) {
			benchmarks.add( Benchmarks.get( id ) );
		}		

		// Setup test solver.
		solver = ResourceLoader.loadSolverIntoDatabase( "CVC4.zip", space.getId(), user.getId() );

		// Setup test job.
		job = ResourceLoader.loadJobIntoDatabase(space.getId(), user.getId(), solver.getId(), benchmarkIds);


	}

	@Override
	protected void teardown() {
		// Delete test space.
		Spaces.removeSubspace( space.getId() );

		// Delete test benchmarks.
		for (Benchmark bench : benchmarks) { 
			Benchmarks.deleteAndRemoveBenchmark( bench.getId() );
		}
		
		// Delete test solver.
		Solvers.deleteAndRemoveSolver( solver.getId() );

		// Delete test job.
		Jobs.deleteAndRemove( job.getId() );
	}
}
