package org.starexec.constants;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class PaginationQueries {
	
	public static String GET_ENQUEUED_PAIRS_QUERY = "";
	
	
	public static void loadPaginationQueries() throws IOException {
		GET_ENQUEUED_PAIRS_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/PairPagination.sql"));
	}
}
