USE prod_starexec;

alter table spaces
modify name varchar(128);

alter table benchmarks
modify name varchar(256);

alter table solvers
modify name varchar(128);

alter table configurations
modify name varchar(128);
