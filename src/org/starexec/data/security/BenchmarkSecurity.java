package org.starexec.data.security;

import java.util.List;

import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;

public class BenchmarkSecurity {
	public static int canUserSeeBenchmarkContents(int benchmarkId, int userId) {
		
		Benchmark b = Benchmarks.get(benchmarkId);
		
		if (b==null) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		//the benchmark doesn't need to be downloadable if this is the owner
		if (b.getUserId()==userId) {
			return 0;
		}
		
		if(Permissions.canUserSeeBench(b.getId(), userId) && b.isDownloadable()) {				
			return 0;				
		} else {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
	}
	
	public static int canUserDeleteBench(int benchId, int userId) {
		
		Benchmark bench = Benchmarks.getIncludeDeletedAndRecycled(benchId,false);
		if (bench==null) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		if(!userOwnsBenchOrIsAdmin(bench,userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	public static int canUserRecycleBench(int benchId, int userId) {
		Benchmark bench = Benchmarks.get(benchId);
		if (bench==null) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		if(!userOwnsBenchOrIsAdmin(bench,userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	public static int canUserRecycleBenchmarks(List<Integer> benchIds, int userId) {
		for (Integer bid : benchIds) {
			if (canUserRecycleBench(bid,userId)!=0) {
				return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
			}
		}
		
		return 0;
	}
	
	public static int canUserDeleteBenchmarks(List<Integer> benchIds, int userId) {
		for (Integer bid : benchIds) {
			if (canUserDeleteBench(bid,userId)!=0) {
				return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
			}
		}
		
		return 0;
	}
	
	public static int canUserRestoreBenchmarks(List<Integer> benchIds, int userId) {
		for (Integer bid : benchIds) {
			if (canUserRestoreBenchmark(bid,userId)!=0) {
				return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
			}
		}
		
		return 0;
	}
	
	
	public static int canUserRestoreBenchmark(int benchId, int userId) {
		Benchmark bench = Benchmarks.getIncludeDeletedAndRecycled(benchId,false);
		if (bench==null) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		if (!Benchmarks.isBenchmarkRecycled(benchId)) {
			return SecurityStatusCodes.ERROR_PRIM_ALREADY_RECYCLED;
		}
		if(!userOwnsBenchOrIsAdmin(bench,userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	public static int canUserEditBenchmark(int benchId, String name, int userId) {
		Benchmark bench = Benchmarks.getIncludeDeletedAndRecycled(benchId,false);
		if (bench==null) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		if(!userOwnsBenchOrIsAdmin(bench,userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		if (Benchmarks.isBenchmarkDeleted(benchId)) {
			return SecurityStatusCodes.ERROR_PRIM_ALREADY_DELETED;
		}
		
		if (!bench.getName().equals(name)) {
			int id=Benchmarks.isNameEditable(benchId);
			if (id<0) {
				return SecurityStatusCodes.ERROR_NAME_NOT_EDITABLE;
			}
			if (id>0 && Spaces.notUniquePrimitiveName(name,id, 2)) {
				return SecurityStatusCodes.ERROR_NOT_UNIQUE_NAME;
			}
		}
		
		
		return 0;
	}
	
	private static boolean userOwnsBenchOrIsAdmin(Benchmark bench,int userId) {
		return (bench.getUserId()==userId || Users.isAdmin(userId));
	}
	
	
}
