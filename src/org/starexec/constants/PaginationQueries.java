package org.starexec.constants;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class PaginationQueries {
	
	public static String GET_ENQUEUED_PAIRS_QUERY = "";
	public static String GET_BENCHMARKS_IN_SPACE_QUERY = "";
	public static String GET_BENCHMARKS_BY_USER_QUERY = "";
	public static String GET_JOBS_IN_SPACE_QUERY = "";
	public static String GET_JOBS_BY_USER_QUERY = "";
	public static String GET_USERS_IN_SPACE_QUERY = "";
	public static String GET_SUBSPACES_IN_SPACE_QUERY = "";

	/**
	 * Reads in the queries stored in the config/pagination package
	 * @throws IOException
	 */
	public static void loadPaginationQueries() throws IOException {
		GET_ENQUEUED_PAIRS_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/PairPagination.sql"));
		GET_BENCHMARKS_IN_SPACE_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/BenchInSpacePagination.sql"));
		GET_BENCHMARKS_BY_USER_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/BenchForUserPagination.sql"));
		GET_JOBS_IN_SPACE_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/JobInSpacePagination.sql"));
		GET_JOBS_BY_USER_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/JobForUserPagination.sql"));
		GET_USERS_IN_SPACE_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/UserInSpacePagination.sql"));
		GET_SUBSPACES_IN_SPACE_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/SubspacesInSpacePagination.sql"));


	}
}
