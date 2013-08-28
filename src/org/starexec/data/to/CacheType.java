package org.starexec.data.to;

public enum CacheType {
		CACHE_SPACE(0),
		CACHE_SPACE_HIERARCHY(1),
		CACHE_SOLVER(2),
		CACHE_BENCHMARK(3),
		CACHE_JOB_OUTPUT(4),
		CACHE_JOB_CSV(5),
		CACHE_SPACE_XML(6);
		
		private int val;
		
		private CacheType(int val) {
			this.val = val;			
		}				
		
		public int getVal() {
			return this.val;			
		}				
		
}
