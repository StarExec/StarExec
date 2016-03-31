-- Author: Todd Elvers
-- Description: This file loads all stored procedure files into the starexec database

USE starexec;

source procedures/AnonymousLinks.sql;
source procedures/Benchmarks.sql;
source procedures/Cluster.sql;
source procedures/Communities.sql;
source procedures/Jobs.sql;
source procedures/Misc.sql;
source procedures/Permissions.sql;
source procedures/Processors.sql;
source procedures/Requests.sql;
source procedures/Reports.sql;
source procedures/Solvers.sql;
source procedures/Spaces.sql;
source procedures/Users.sql;
source procedures/Websites.sql;
source procedures/JobPairs.sql;
source procedures/Queues.sql;
source procedures/Settings.sql;
source procedures/UploadStatus.sql;
source procedures/Pipelines.sql;

source procedures/pagination/RequestPagination.sql;
