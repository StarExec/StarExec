-- Author: Todd Elvers
-- Description: This file loads all stored procedure files into the starexec database

USE starexec;

source procedures/Benchmarks.sql;
source procedures/Cluster.sql;
source procedures/Communities.sql;
source procedures/Jobs.sql;
source procedures/Misc.sql;
source procedures/Permissions.sql;
source procedures/Processors.sql;
source procedures/Requests.sql;
source procedures/Solvers.sql;
source procedures/Spaces.sql;
source procedures/Users.sql;
source procedures/Websites.sql;
source procedures/JobPairs.sql;
source procedures/Queues.sql;
source procedures/Cache.sql;
source procedures/pagination/PairPagination.sql;
source procedures/pagination/JobPagination.sql;
source procedures/pagination/SolverPagination.sql;
source procedures/pagination/BenchmarkPagination.sql;
source procedures/pagination/UserPagination.sql;
source procedures/pagination/SpacePagination.sql;