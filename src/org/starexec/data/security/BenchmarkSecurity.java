package org.starexec.data.security;

import java.util.List;

import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.util.Validator;

public class BenchmarkSecurity {
	
	/**
	 * Checks to see whether the given user is allowed to see the contents of the given 
	 * benchmark
	 * @param benchmarkId The ID of the benchmark being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if allowed, or a status code from ValidatorStatusCodes if not allowed.
	 */	
	public static ValidatorStatusCode canUserSeeBenchmarkContents(int benchmarkId, int userId) {
		
		Benchmark b = Benchmarks.get(benchmarkId);
		
		if (b==null) {
			
			return new ValidatorStatusCode(false, "The benchmark could not be found");
		}
		//the benchmark doesn't need to be downloadable if this is the owner
		if (b.getUserId()==userId || Users.isAdmin(userId)) {
			return new ValidatorStatusCode(true);
		}
		
		if(Permissions.canUserSeeBench(b.getId(), userId) && b.isDownloadable()) {				
			return new ValidatorStatusCode(true);
			
		} else {
			return new ValidatorStatusCode(false, "You do not have permission to see the contents of this benchmark");
		}
	}
	
	
	/**
	 * Checks to see whether the given user is allowed to delete the given benchmark
	 * @param benchmarkId The ID of the benchmark being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if allowed, or a status code from ValidatorStatusCodes if not allowed.
	 */
	
	public static ValidatorStatusCode canUserDeleteBench(int benchId, int userId) {
		
		Benchmark bench = Benchmarks.getIncludeDeletedAndRecycled(benchId,false);
		if (bench==null) {
			
			return new ValidatorStatusCode(false, "The benchmark could not be found");
		}
		if(!userOwnsBenchOrIsAdmin(bench,userId)){
			return new ValidatorStatusCode(false, "You do not have permission to delete this benchmark");
		}
		return new ValidatorStatusCode(true);
	}
	
	
	/**
	 * Checks to see whether the given user is allowed to recycle the given benchmark
	 * @param benchmarkId The ID of the benchmark being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if allowed, or a status code from ValidatorStatusCodes if not allowed.
	 */
	
	public static ValidatorStatusCode canUserRecycleBench(int benchId, int userId) {
		Benchmark bench = Benchmarks.get(benchId);
		if (bench==null) {
			return new ValidatorStatusCode(false, "The benchmark could not be found");
		}
		if(!userOwnsBenchOrIsAdmin(bench,userId)){
			return new ValidatorStatusCode(false, "You do not have permission to recycle this benchmark");
		}
		return new ValidatorStatusCode(true);
	}
	
	
	
	/**
	 * Checks to see whether the given user is allowed to recycle all of the given benchmarks
	 * @param benchmarkId The ID of the benchmark being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if allowed, or a status code from ValidatorStatusCodes if not allowed. 
	 * If the user doesn't have the required permissions for even 1 benchmark, a status
	 * code will be returned
	 */
	public static ValidatorStatusCode canUserRecycleBenchmarks(List<Integer> benchIds, int userId) {
		for (Integer bid : benchIds) {
			ValidatorStatusCode code=canUserRecycleBench(bid,userId);
			if (!code.isSuccess()) {
				return code;
			}
			
		}
		
		return new ValidatorStatusCode(true);
	}
	/**
	 * Checks to see whether the given user is allowed to delete all of the given benchmarks
	 * @param benchmarkId The ID of the benchmark being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if allowed, or a status code from ValidatorStatusCodes if not allowed. 
	 * If the user doesn't have the required permissions for even 1 benchmark, a status
	 * code will be returned
	 */
	public static ValidatorStatusCode canUserDeleteBenchmarks(List<Integer> benchIds, int userId) {
		for (Integer bid : benchIds) {
			ValidatorStatusCode code=canUserDeleteBench(bid,userId);
			if (!code.isSuccess()) {
				return code;
			}
		}
		
		return new ValidatorStatusCode(true);
	}
	
	
	/**
	 * Checks to see whether the given user is allowed to restore all of the given benchmarks
	 * @param benchmarkId The ID of the benchmark being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if allowed, or a status code from ValidatorStatusCodes if not allowed. 
	 * If the user doesn't have the required permissions for even 1 benchmark, a status
	 * code will be returned
	 */
	public static ValidatorStatusCode canUserRestoreBenchmarks(List<Integer> benchIds, int userId) {
		for (Integer bid : benchIds) {
			ValidatorStatusCode code=canUserRestoreBenchmark(bid,userId);
			if (!code.isSuccess()) {
				return code;
			}
		}
		
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user is allowed to restore the given benchmark
	 * @param benchmarkId The ID of the benchmark being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if allowed, or a status code from ValidatorStatusCodes if not allowed.
	 */
	public static ValidatorStatusCode canUserRestoreBenchmark(int benchId, int userId) {
		Benchmark bench = Benchmarks.getIncludeDeletedAndRecycled(benchId,false);
		if (bench==null) {
			return new ValidatorStatusCode(false, "The benchmark could not be found");
		}
		if (!Benchmarks.isBenchmarkRecycled(benchId)) {
			return new ValidatorStatusCode(false, "The benchmark is not currently recycled, so it cannot be restored");
		}
		if(!userOwnsBenchOrIsAdmin(bench,userId)){
			return new ValidatorStatusCode(false, "You do not have permission to restore this benchmark");
		}
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user is allowed to edit the given benchmark
	 * @param benchmarkId The ID of the benchmark being checked
	 * @param name The name that the benchmark will be given upon editing
	 * @param userId The ID of the user making the request
	 * @return 0 if allowed, or a status code from ValidatorStatusCodes if not allowed.
	 */
	
	public static ValidatorStatusCode canUserEditBenchmark(int benchId, String name,String desc, int userId) {
		// Ensure the parameters are valid
		if(!Validator.isValidBenchName(name)) { 
			return new ValidatorStatusCode(false, "The new name is not valid. Please refer to the help pages to find format for benchmark names");
		}
		
		if(!Validator.isValidPrimDescription(desc)) { 
			return new ValidatorStatusCode(false, "The new description is not valid. Please refer to the help pages to find format for benchmark descriptions");
		}
		
		Benchmark bench = Benchmarks.getIncludeDeletedAndRecycled(benchId,false);
		if (bench==null) {
			return new ValidatorStatusCode(false, "The benchmark could not be found");
		}
		if(!userOwnsBenchOrIsAdmin(bench,userId)){
			return new ValidatorStatusCode(false, "You do not have permission to edit this benchmark");
		}
		if (Benchmarks.isBenchmarkDeleted(benchId)) {
			return new ValidatorStatusCode(false, "The benchmark has been deleted");
		}
		
		if (!bench.getName().equals(name)) {
			int id=Benchmarks.isNameEditable(benchId);
			if (id<0) {
				return new ValidatorStatusCode(false, "The benchmark is in more than one space, so its name cannot be edited.");
			}
			if (id>0 && Spaces.notUniquePrimitiveName(name,id, 2)) {
				return new ValidatorStatusCode(false, "The new name must be unique of all benchmarks in the space");
			}
		}
		
		
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Returns true if the given user owns the given benchmark or if the user is an admin
	 * @param benchmarkId The ID of the benchmark being checked
	 * @param userId The ID of the user making the request
	 */
	
	private static boolean userOwnsBenchOrIsAdmin(Benchmark bench,int userId) {
		return (bench.getUserId()==userId || Users.isAdmin(userId));
	}
	
	/**
	 * Checks to see whether a user can recycle all the orphaned benchmarks owned by another user.
	 * @param userIdToDelete The user who owns the orphaned benchmarks that will be recycled
	 * @param userIdMakingRequest The user who is trying to do the recycling
	 * @return A ValidatorStatusCode
	 */
	public static ValidatorStatusCode canUserRecycleOrphanedBenchmarks(int userIdToDelete, int userIdMakingRequest) {
		if (userIdToDelete!=userIdMakingRequest && !Users.isAdmin(userIdMakingRequest)) {
			return new ValidatorStatusCode(false, "You do not have permission to recycle benchmarks belonging to another user");
		}
		
		return new ValidatorStatusCode(true);
	}
	
	
}
