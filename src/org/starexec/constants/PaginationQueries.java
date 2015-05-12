package org.starexec.constants;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class PaginationQueries {
	
	public static String GET_PAIRS_IN_SPACE_QUERY = "";
	public static String GET_PAIRS_IN_SPACE_HIERARCHY_QUERY = "";

	public static String GET_BENCHMARKS_IN_SPACE_QUERY = "";
	public static String GET_BENCHMARKS_BY_USER_QUERY = "";
	public static String GET_JOBS_IN_SPACE_QUERY = "";
	public static String GET_JOBS_BY_USER_QUERY = "";
	public static String GET_USERS_IN_SPACE_QUERY = "";
	public static String GET_SUBSPACES_IN_SPACE_QUERY = "";
	public static String GET_INCOMPLETE_JOBS_QUERY = "";
	public static String GET_SOLVERS_IN_SPACE_QUERY = "";
	public static String GET_SOLVERS_BY_USER_QUERY = "";
	public static String GET_PAIRS_RUNNING_QUERY = "";
	public static String GET_PAIRS_ENQUEUED_QUERY = "";

	/**
	 * Reads in the queries stored in the config/pagination package
	 * @throws IOException
	 */
	public static void loadPaginationQueries() throws IOException {
		GET_PAIRS_IN_SPACE_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/PairInJobSpacePagination.sql"));
		GET_BENCHMARKS_IN_SPACE_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/BenchInSpacePagination.sql"));
		GET_BENCHMARKS_BY_USER_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/BenchForUserPagination.sql"));
		GET_JOBS_IN_SPACE_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/JobInSpacePagination.sql"));
		GET_JOBS_BY_USER_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/JobForUserPagination.sql"));
		GET_USERS_IN_SPACE_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/UserInSpacePagination.sql"));
		GET_SUBSPACES_IN_SPACE_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/SubspacesInSpacePagination.sql"));
		GET_INCOMPLETE_JOBS_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/IncompleteJobPagination.sql"));
		GET_SOLVERS_IN_SPACE_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/SolverInSpacePagination.sql"));
		GET_SOLVERS_BY_USER_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/SolverForUserPagination.sql"));
		GET_PAIRS_IN_SPACE_HIERARCHY_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/PairInJobSpaceHierarchyPagination.sql"));
		GET_PAIRS_RUNNING_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/RunningPairPagination.sql"));
		GET_PAIRS_ENQUEUED_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/pagination/EnqueuedPairPagination.sql"));


	}
}
