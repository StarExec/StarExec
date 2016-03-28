package org.starexec.data.security;

import java.util.List;

import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Uploads;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.BenchmarkUploadStatus;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.util.Validator;

public class BenchmarkSecurity {
	
	/**
	 * Checks to see whether the given user has permission to download the given benchmark
	 * @param benchId
	 * @param userId
	 * @return
	 */
	
	public static ValidatorStatusCode canUserDownloadBenchmark(int benchId, int userId) {
		Benchmark b=Benchmarks.get(benchId);
		if (b==null) {
			return new ValidatorStatusCode(false, "The given benchmark could not be found");
		}
		if (!Permissions.canUserSeeBench(benchId, userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to see the given benchmark");
		
		}
		if (!(b.isDownloadable() || b.getUserId()==userId || GeneralSecurity.hasAdminReadPrivileges(userId))) {
			return new ValidatorStatusCode(false, "The given benchmark has been marked as being not downloadable");
		}
		
		return new ValidatorStatusCode(true);
		
	}

	/**
	 * Checks to see whether the given userId can get an anonymous public link for the benchmark.
	 * @param benchmarkId The id of the benchmark for which an anonymous link is being requested.
	 * @param userId The id of the user attempting to generate an anonymous link.
	 * @return A status code indicating if the user can generate a link.
	 * @author Albert Giegerich
	 */
	public static ValidatorStatusCode canUserGetAnonymousLink( int benchmarkId, int userId ) {
		Benchmark bench = Benchmarks.get( benchmarkId );
		if ( !userOwnsBenchOrIsAdmin( bench, userId )) {
			return new ValidatorStatusCode( false, "You do not have permission to generate an anonymous link for this benchmark." );
		} else { 	
			return new ValidatorStatusCode( true );
		}
	}
	
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
		if (b.getUserId()==userId || GeneralSecurity.hasAdminReadPrivileges(userId)) {
			return new ValidatorStatusCode(true);
		}
		
		if(Permissions.canUserSeeBench(b.getId(), userId) && b.isDownloadable()) {				
			return new ValidatorStatusCode(true);	
		}
		return new ValidatorStatusCode(false, "You do not have permission to see the contents of this benchmark");
		
	}
	
	
	/**
	 * Checks to see whether the given user is allowed to delete the given benchmark
	 * @param benchId The ID of the benchmark being checked
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
	 * @param benchId The ID of the benchmark being checked
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
	 * @param benchIds The IDs of all the benchmarks being checked
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
	 * @param benchIds The IDs of all the benchmarks being checked
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
	 * @param benchIds The IDs of all the benchmarks being checked
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
	 * @param benchId The ID of the benchmark being checked
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
	 * @param benchId The ID of the benchmark being checked
	 * @param name The name that the benchmark will be given upon editing
	 * @param desc the description that will be given to the benchmark upon editing
	 * @param typeId The new benchmark type to assign to the benchmark
	 * @param userId The ID of the user making the request
	 * @return 0 if allowed, or a status code from ValidatorStatusCodes if not allowed.
	 */
	
	public static ValidatorStatusCode canUserEditBenchmark(int benchId, String name,String desc,int typeId, int userId) {
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
		
		Processor p = Processors.get(typeId);
		if (p==null) {
			return new ValidatorStatusCode(false, "The given type could not be found");
		} else if (p.getType()!=ProcessorType.BENCH) {
			return new ValidatorStatusCode(false, "The given type is not a benchmark processor type");
		}
		return ProcessorSecurity.canUserSeeProcessor(typeId, userId);
	}
	
	/**
	 * Returns true if the given user owns the given benchmark or if the user is an admin
	 * @param bench The  Benchmark object being checked, which must have its userId field set
	 * @param userId The ID of the user making the request
	 * @return True of the user is the owner of the benchmark or an admin
	 */
	
	private static boolean userOwnsBenchOrIsAdmin(Benchmark bench,int userId) {
		return (bench!=null && (bench.getUserId()==userId || GeneralSecurity.hasAdminWritePrivileges(userId)));
	}
	
	/**
	 * Checks to see whether a user can recycle all the orphaned benchmarks owned by another user.
	 * @param userIdToDelete The user who owns the orphaned benchmarks that will be recycled
	 * @param userIdMakingRequest The user who is trying to do the recycling
	 * @return A ValidatorStatusCode
	 */
	public static ValidatorStatusCode canUserRecycleOrphanedBenchmarks(int userIdToDelete, int userIdMakingRequest) {
		if (userIdToDelete!=userIdMakingRequest && !GeneralSecurity.hasAdminWritePrivileges(userIdMakingRequest)) {
			return new ValidatorStatusCode(false, "You do not have permission to recycle benchmarks belonging to another user");
		}
		
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see whether the user is allowed to download the Json object representing the benchmark
	 * @param benchmarkId
	 * @param userId
	 * @return
	 */
	public static ValidatorStatusCode canGetJsonBenchmark(int benchmarkId, int userId) {
		if (!Permissions.canUserSeeBench(benchmarkId, userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to see the specified benchmark");
		}
		Benchmark b=Benchmarks.getIncludeDeletedAndRecycled(benchmarkId,false);
		if (b==null) {
			return new ValidatorStatusCode(false, "The given benchmark could not be found");
		}
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see if the user belongs to the given upload status
	 * @param statusId The space to check if the user can see
	 * @param userId The user that is requesting to view the given upload status
	 * @return True if the user owns the status, false otherwise
	 * @author Benton McCune
	 */
	public static boolean canUserSeeBenchmarkStatus(int statusId, int userId) {		
		
		BenchmarkUploadStatus status=Uploads.getBenchmarkStatus(statusId);
		if (status==null) {
			return false;
		}
		if (GeneralSecurity.hasAdminReadPrivileges(userId)) {
			return true;
		}
		return status.getUserId()==userId;
	}
}
