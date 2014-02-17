package org.starexec.data.to;

public enum CacheType {
		CACHE_SPACE(0),
		CACHE_SPACE_HIERARCHY(1),
		CACHE_SOLVER(2),
		CACHE_BENCHMARK(3),
		CACHE_JOB_OUTPUT(4),
		CACHE_JOB_CSV(5),
		CACHE_SPACE_XML(6),
		CACHE_JOB_CSV_NO_IDS(7),
		CACHE_SOLVER_REUPLOAD(8),
		CACHE_JOB_PAIR(9);
		private int val;
		
		private CacheType(int val) {
			this.val = val;			
		}				
		
		public int getVal() {
			return this.val;			
		}
		public static CacheType getType(int val) {
			switch (val) {
				case 0:
					return CacheType.CACHE_SPACE;
				case 1:
					return CacheType.CACHE_SPACE_HIERARCHY;
				case 2:
					return CacheType.CACHE_SOLVER;
				case 3: 
					return CacheType.CACHE_BENCHMARK;
				case 4:
					return CacheType.CACHE_JOB_OUTPUT;
				case 5:
					return CacheType.CACHE_JOB_CSV;
				case 6:
					return CacheType.CACHE_SPACE_XML;
				case 7:
					return CacheType.CACHE_JOB_CSV_NO_IDS;
				case 8:
					return CacheType.CACHE_SOLVER_REUPLOAD;
				case 9:
					return CacheType.CACHE_JOB_PAIR;
			}
				
			return null;
		}
		
}
