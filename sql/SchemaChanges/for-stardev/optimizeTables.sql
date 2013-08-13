USE Starexec;

CALL SetNameColumns();

OPTIMIZE TABLE  job_pairs;
OPTIMIZE TABLE  spaces;
OPTIMIZE TABLE 	solvers;
OPTIMIZE TABLE  configurations;
OPTIMIZE TABLE  benchmarks;
OPTIMIZE TABLE  jobs;
OPTIMIZE TABLE  users;