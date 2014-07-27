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
	 * @return 0 if allowed, or a status code from SecurityStatusCodes if not allowed.
	 */	
	public static SecurityStatusCode canUserSeeBenchmarkContents(int benchmarkId, int userId) {
		
		Benchmark b = Benchmarks.get(benchmarkId);
		
		if (b==null) {
			
			return new SecurityStatusCode(false, "The benchmark could not be found");
		}
		//the benchmark doesn't need to be downloadable if this is the owner
		if (b.getUserId()==userId || Users.isAdmin(userId)) {
			return new SecurityStatusCode(true);
		}
		
		if(Permissions.canUserSeeBench(b.getId(), userId) && b.isDownloadable()) {				
			return new SecurityStatusCode(true);
			
		} else {
			return new SecurityStatusCode(false, "You do not have permission to see the contents of this benchmark");
		}
	}
	
	
	/**
	 * Checks to see whether the given user is allowed to delete the given benchmark
	 * @param benchmarkId The ID of the benchmark being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if allowed, or a status code from SecurityStatusCodes if not allowed.
	 */
	
	public static SecurityStatusCode canUserDeleteBench(int benchId, int userId) {
		
		Benchmark bench = Benchmarks.getIncludeDeletedAndRecycled(benchId,false);
		if (bench==null) {
			
			return new SecurityStatusCode(false, "The benchmark could not be found");
		}
		if(!userOwnsBenchOrIsAdmin(bench,userId)){
			return new SecurityStatusCode(false, "You do not have permission to delete this benchmark");
		}
		return new SecurityStatusCode(true);
	}
	
	
	/**
	 * Checks to see whether the given user is allowed to recycle the given benchmark
	 * @param benchmarkId The ID of the benchmark being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if allowed, or a status code from SecurityStatusCodes if not allowed.
	 */
	
	public static SecurityStatusCode canUserRecycleBench(int benchId, int userId) {
		Benchmark bench = Benchmarks.get(benchId);
		if (bench==null) {
			return new SecurityStatusCode(false, "The benchmark could not be found");
		}
		if(!userOwnsBenchOrIsAdmin(bench,userId)){
			return new SecurityStatusCode(false, "You do not have permission to recycle this benchmark");
		}
		return new SecurityStatusCode(true);
	}
	
	
	
	/**
	 * Checks to see whether the given user is allowed to recycle all of the given benchmarks
	 * @param benchmarkId The ID of the benchmark being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if allowed, or a status code from SecurityStatusCodes if not allowed. 
	 * If the user doesn't have the required permissions for even 1 benchmark, a status
	 * code will be returned
	 */
	public static SecurityStatusCode canUserRecycleBenchmarks(List<Integer> benchIds, int userId) {
		for (Integer bid : benchIds) {
			SecurityStatusCode code=canUserRecycleBench(bid,userId);
			if (!code.isSuccess()) {
				return code;
			}
			
		}
		
		return new SecurityStatusCode(true);
	}
	/**
	 * Checks to see whether the given user is allowed to delete all of the given benchmarks
	 * @param benchmarkId The ID of the benchmark being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if allowed, or a status code from SecurityStatusCodes if not allowed. 
	 * If the user doesn't have the required permissions for even 1 benchmark, a status
	 * code will be returned
	 */
	public static SecurityStatusCode canUserDeleteBenchmarks(List<Integer> benchIds, int userId) {
		for (Integer bid : benchIds) {
			SecurityStatusCode code=canUserDeleteBench(bid,userId);
			if (!code.isSuccess()) {
				return code;
			}
		}
		
		return new SecurityStatusCode(true);
	}
	
	
	/**
	 * Checks to see whether the given user is allowed to restore all of the given benchmarks
	 * @param benchmarkId The ID of the benchmark being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if allowed, or a status code from SecurityStatusCodes if not allowed. 
	 * If the user doesn't have the required permissions for even 1 benchmark, a status
	 * code will be returned
	 */
	public static SecurityStatusCode canUserRestoreBenchmarks(List<Integer> benchIds, int userId) {
		for (Integer bid : benchIds) {
			SecurityStatusCode code=canUserRestoreBenchmark(bid,userId);
			if (!code.isSuccess()) {
				return code;
			}
		}
		
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user is allowed to restore the given benchmark
	 * @param benchmarkId The ID of the benchmark being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if allowed, or a status code from SecurityStatusCodes if not allowed.
	 */
	public static SecurityStatusCode canUserRestoreBenchmark(int benchId, int userId) {
		Benchmark bench = Benchmarks.getIncludeDeletedAndRecycled(benchId,false);
		if (bench==null) {
			return new SecurityStatusCode(false, "The benchmark could not be found");
		}
		if (!Benchmarks.isBenchmarkRecycled(benchId)) {
			return new SecurityStatusCode(false, "The benchmark is not currently recycled, so it cannot be restored");
		}
		if(!userOwnsBenchOrIsAdmin(bench,userId)){
			return new SecurityStatusCode(false, "You do not have permission to restore this benchmark");
		}
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user is allowed to edit the given benchmark
	 * @param benchmarkId The ID of the benchmark being checked
	 * @param name The name that the benchmark will be given upon editing
	 * @param userId The ID of the user making the request
	 * @return 0 if allowed, or a status code from SecurityStatusCodes if not allowed.
	 */
	
	public static SecurityStatusCode canUserEditBenchmark(int benchId, String name,String desc, int userId) {
		// Ensure the parameters are valid
		if(!Validator.isValidBenchName(name)) { 
			return new SecurityStatusCode(false, "The new name is not valid. Please refer to the help pages to find format for benchmark names");
		}
		
		if(!Validator.isValidPrimDescription(desc)) { 
			return new SecurityStatusCode(false, "The new description is not valid. Please refer to the help pages to find format for benchmark descriptions");
		}
		
		Benchmark bench = Benchmarks.getIncludeDeletedAndRecycled(benchId,false);
		if (bench==null) {
			return new SecurityStatusCode(false, "The benchmark could not be found");
		}
		if(!userOwnsBenchOrIsAdmin(bench,userId)){
			return new SecurityStatusCode(false, "You do not have permission to edit this benchmark");
		}
		if (Benchmarks.isBenchmarkDeleted(benchId)) {
			return new SecurityStatusCode(false, "The benchmark has been deleted");
		}
		
		if (!bench.getName().equals(name)) {
			int id=Benchmarks.isNameEditable(benchId);
			if (id<0) {
				return new SecurityStatusCode(false, "The benchmark is in more than one space, so its name cannot be edited.");
			}
			if (id>0 && Spaces.notUniquePrimitiveName(name,id, 2)) {
				return new SecurityStatusCode(false, "The new name must be unique of all benchmarks in the space");
			}
		}
		
		
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Returns true if the given user owns the given benchmark or if the user is an admin
	 * @param benchmarkId The ID of the benchmark being checked
	 * @param userId The ID of the user making the request
	 */
	
	private static boolean userOwnsBenchOrIsAdmin(Benchmark bench,int userId) {
		return (bench.getUserId()==userId || Users.isAdmin(userId));
	}
	
	
}
