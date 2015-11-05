use starexec;

ALTER TABLE benchmarks DROP FOREIGN KEY benchmarks_user_id;
ALTER TABLE benchmarks ADD CONSTRAINT benchmarks_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE solvers DROP FOREIGN KEY solvers_user_id;
ALTER TABLE solvers ADD CONSTRAINT FOREIGN KEY solvers_user_id (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE jobs DROP FOREIGN KEY jobs_user_id;
ALTER TABLE jobs ADD CONSTRAINT FOREIGN KEY jobs_user_id (user_id) REFERENCES users(id) ON DELETE CASCADE;
