package org.starexec.constants;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class PaginationQueries {
	private static final String GET_PAIRS_IN_SPACE_PATH = "/pagination/PairInJobSpacePagination.sql";
	public static String GET_PAIRS_IN_SPACE_QUERY = "";

	private static final String GET_PAIRS_IN_SPACE_HIERARCHY_PATH = "/pagination/PairInJobSpaceHierarchyPagination.sql";
	public static String GET_PAIRS_IN_SPACE_HIERARCHY_QUERY = "";

	private static final String GET_BENCHMARKS_IN_SPACE_PATH = "/pagination/BenchInSpacePagination.sql";
	public static String GET_BENCHMARKS_IN_SPACE_QUERY = "";

	private static final String GET_BENCHMARKS_BY_USER_PATH = "/pagination/BenchForUserPagination.sql";
	public static String GET_BENCHMARKS_BY_USER_QUERY = "";

	private static final String GET_JOBS_IN_SPACE_PATH = "/pagination/JobInSpacePagination.sql";
	public static String GET_JOBS_IN_SPACE_QUERY = "";

	private static final String GET_JOBS_BY_USER_PATH = "/pagination/JobForUserPagination.sql";
	public static String GET_JOBS_BY_USER_QUERY = "";

	private static final String GET_USERS_IN_SPACE_PATH ="/pagination/UserInSpacePagination.sql";
	public static String GET_USERS_IN_SPACE_QUERY = "";

	private static final String GET_SUBSPACES_IN_SPACE_PATH = "/pagination/SubspacesInSpacePagination.sql";
	public static String GET_SUBSPACES_IN_SPACE_QUERY = "";

	private static final String GET_SOLVERS_IN_SPACE_PATH = "/pagination/SolverInSpacePagination.sql";
	public static String GET_SOLVERS_IN_SPACE_QUERY = "";

	private static final String GET_SOLVERS_BY_USER_PATH ="/pagination/SolverForUserPagination.sql";
	public static String GET_SOLVERS_BY_USER_QUERY = "";

	private static final String GET_PAIRS_ENQUEUED_PATH = "/pagination/EnqueuedPairPagination.sql";
	public static String GET_PAIRS_ENQUEUED_QUERY = "";

	private static final String GET_USERS_ADMIN_PATH = "/pagination/UsersForAdmin.sql";
	public static String GET_USERS_ADMIN_QUERY = "";
    
        private static final String GET_UPLOADS_BY_USER_PATH = "/pagination/UploadForUserPagination.sql";
        public static String GET_UPLOADS_BY_USER_QUERY = "";

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
		GET_SOLVERS_IN_SPACE_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, GET_SOLVERS_IN_SPACE_PATH));
		GET_SOLVERS_BY_USER_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, GET_SOLVERS_BY_USER_PATH));
		GET_PAIRS_IN_SPACE_HIERARCHY_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, GET_PAIRS_IN_SPACE_HIERARCHY_PATH));
		GET_PAIRS_ENQUEUED_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, GET_PAIRS_ENQUEUED_PATH));
		GET_USERS_ADMIN_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, GET_USERS_ADMIN_PATH));
		GET_UPLOADS_BY_USER_QUERY = FileUtils.readFileToString(new File(R.CONFIG_PATH, GET_UPLOADS_BY_USER_PATH));
	}
}
