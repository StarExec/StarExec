package org.starexec.constants;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class PaginationQueries {
	
	public static String GET_PAIRS_IN_SPACE_PATH = "/pagination/PairInJobSpacePagination.sql";
	public static String GET_PAIRS_IN_SPACE_QUERY = "";

	public static String GET_PAIRS_IN_SPACE_HIERARCHY_PATH = "/pagination/PairInJobSpaceHierarchyPagination.sql";
	public static String GET_PAIRS_IN_SPACE_HIERARCHY_QUERY = "";

	public static String GET_BENCHMARKS_IN_SPACE_PATH = "/pagination/BenchInSpacePagination.sql";
	public static String GET_BENCHMARKS_IN_SPACE_QUERY = "";

	public static String GET_BENCHMARKS_BY_USER_PATH = "/pagination/BenchForUserPagination.sql";
	public static String GET_BENCHMARKS_BY_USER_QUERY = "";

	public static String GET_JOBS_IN_SPACE_PATH = "/pagination/JobInSpacePagination.sql";
	public static String GET_JOBS_IN_SPACE_QUERY = "";

	public static String GET_JOBS_BY_USER_PATH = "/pagination/JobForUserPagination.sql";
	public static String GET_JOBS_BY_USER_QUERY = "";

	public static String GET_USERS_IN_SPACE_PATH ="/pagination/UserInSpacePagination.sql";
	public static String GET_USERS_IN_SPACE_QUERY = "";

	public static String GET_SUBSPACES_IN_SPACE_PATH = "/pagination/SubspacesInSpacePagination.sql";
	public static String GET_SUBSPACES_IN_SPACE_QUERY = "";

	public static String GET_INCOMPLETE_JOBS_PATH = "/pagination/IncompleteJobPagination.sql";
	public static String GET_INCOMPLETE_JOBS_QUERY = "";

	public static String GET_SOLVERS_IN_SPACE_PATH = "/pagination/SolverInSpacePagination.sql";
	public static String GET_SOLVERS_IN_SPACE_QUERY = "";

	public static String GET_SOLVERS_BY_USER_PATH ="/pagination/SolverForUserPagination.sql";
	public static String GET_SOLVERS_BY_USER_QUERY = "";

	public static String GET_PAIRS_ENQUEUED_PATH = "/pagination/EnqueuedPairPagination.sql";
	public static String GET_PAIRS_ENQUEUED_QUERY = "";

	/**
	 * Reads in the queries stored in the config/pagination package
	 * @throws IOException
	 */
	public static void loadPaginationQueries() throws IOException {
		GET_PAIRS_IN_SPACE_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, GET_PAIRS_IN_SPACE_PATH));
		GET_BENCHMARKS_IN_SPACE_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, GET_BENCHMARKS_IN_SPACE_PATH));
		GET_BENCHMARKS_BY_USER_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, GET_BENCHMARKS_BY_USER_PATH));
		GET_JOBS_IN_SPACE_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, GET_JOBS_IN_SPACE_PATH));
		GET_JOBS_BY_USER_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, GET_JOBS_BY_USER_PATH));
		GET_USERS_IN_SPACE_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, GET_USERS_IN_SPACE_PATH));
		GET_SUBSPACES_IN_SPACE_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, GET_SUBSPACES_IN_SPACE_PATH));
		GET_INCOMPLETE_JOBS_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, GET_INCOMPLETE_JOBS_PATH));
		GET_SOLVERS_IN_SPACE_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, GET_SOLVERS_IN_SPACE_PATH));
		GET_SOLVERS_BY_USER_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, GET_SOLVERS_BY_USER_PATH));
		GET_PAIRS_IN_SPACE_HIERARCHY_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, GET_PAIRS_IN_SPACE_HIERARCHY_PATH));
		GET_PAIRS_ENQUEUED_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, GET_PAIRS_ENQUEUED_PATH));
	}
}
