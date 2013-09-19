USE starexec;

DROP TABLE queue_assoc;

CREATE TABLE queue_assoc (
	queue_id INT NOT NULL, 	
	node_id INT NOT NULL,	
	PRIMARY KEY (queue_id, node_id),
	CONSTRAINT queue_assoc_queue_id FOREIGN KEY (queue_id) REFERENCES queues(id) ON DELETE CASCADE,
	CONSTRAINT queue_assoc_node_id FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE CASCADE
);


ALTER TABLE user_roles ADD CONSTRAINT user_roles_email FOREIGN KEY (email) REFERENCES users(email)  ON DELETE CASCADE; 
ALTER TABLE logins ADD CONSTRAINT logins_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION; 
ALTER TABLE spaces ADD CONSTRAINT spaces_default_permission FOREIGN KEY (default_permission) REFERENCES permissions(id) ON DELETE SET NULL; 
ALTER TABLE closure ADD CONSTRAINT closure_ancestor FOREIGN KEY (ancestor) REFERENCES spaces(id) ON DELETE CASCADE; 
ALTER TABLE closure ADD CONSTRAINT closure_descendant FOREIGN KEY (descendant) REFERENCES spaces(id) ON DELETE CASCADE; 
ALTER TABLE processors ADD CONSTRAINT processors_community FOREIGN KEY (community) REFERENCES spaces(id) ON DELETE CASCADE; 
ALTER TABLE benchmarks ADD CONSTRAINT benchmarks_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION; 
ALTER TABLE benchmarks ADD CONSTRAINT benchmarks_bench_type FOREIGN KEY (bench_type) REFERENCES processors(id) ON DELETE SET NULL; 
ALTER TABLE bench_attributes ADD CONSTRAINT bench_attributes_bench_id FOREIGN KEY (bench_id) REFERENCES benchmarks(id) ON DELETE CASCADE; 
ALTER TABLE solvers ADD CONSTRAINT solvers_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION; 
ALTER TABLE jobs ADD CONSTRAINT jobs_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION; 
ALTER TABLE jobs ADD CONSTRAINT jobs_queue_id FOREIGN KEY (queue_id) REFERENCES queues(id) ON DELETE SET NULL; 
ALTER TABLE jobs ADD CONSTRAINT jobs_pre_processor FOREIGN KEY (pre_processor) REFERENCES processors(id) ON DELETE SET NULL; 
ALTER TABLE jobs ADD CONSTRAINT jobs_post_processor FOREIGN KEY (post_processor) REFERENCES processors(id) ON DELETE SET NULL; 
ALTER TABLE configurations ADD CONSTRAINT configurations_solver_id FOREIGN KEY (solver_id) REFERENCES solvers(id) ON DELETE CASCADE; 
ALTER TABLE job_pairs ADD CONSTRAINT job_pairs_job_id FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE; 
ALTER TABLE job_pairs ADD CONSTRAINT job_pairs_node_id FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE NO ACTION; 
ALTER TABLE job_pairs ADD CONSTRAINT job_pairs_solver_id FOREIGN KEY (solver_id) REFERENCES solvers(id) ON DELETE SET NULL; 
ALTER TABLE job_pair_completion ADD CONSTRAINT job_pair_completion_pair_id FOREIGN KEY (pair_id) REFERENCES job_pairs(id) ON DELETE CASCADE; 
ALTER TABLE job_attributes ADD CONSTRAINT job_attributes_pair_id FOREIGN KEY (pair_id) REFERENCES job_pairs(id) ON DELETE CASCADE; 
ALTER TABLE verify ADD CONSTRAINT verify_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE; 
ALTER TABLE website ADD CONSTRAINT website_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE; 
ALTER TABLE website ADD CONSTRAINT website_solver_id FOREIGN KEY (solver_id) REFERENCES solvers(id) ON DELETE CASCADE; 
ALTER TABLE website ADD CONSTRAINT website_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE; 
ALTER TABLE user_assoc ADD CONSTRAINT user_assoc_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE; 
ALTER TABLE user_assoc ADD CONSTRAINT user_assoc_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE; 
ALTER TABLE user_assoc ADD CONSTRAINT user_assoc_permission FOREIGN KEY (permission) REFERENCES permissions(id) ON DELETE SET NULL; 
ALTER TABLE set_assoc ADD CONSTRAINT set_assoc_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE; 
ALTER TABLE set_assoc ADD CONSTRAINT set_assoc_child_id FOREIGN KEY (child_id) REFERENCES spaces(id) ON DELETE CASCADE; 
ALTER TABLE bench_assoc ADD CONSTRAINT bench_assoc_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE; 
ALTER TABLE bench_assoc ADD CONSTRAINT bench_assoc_bench_id FOREIGN KEY (bench_id) REFERENCES benchmarks(id) ON DELETE CASCADE; 
ALTER TABLE job_assoc ADD CONSTRAINT job_assoc_space_id FOREIGN KEY(space_id) REFERENCES spaces(id) ON DELETE CASCADE; 
ALTER TABLE job_assoc ADD CONSTRAINT job_assoc_job_id FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE; 
ALTER TABLE solver_assoc ADD CONSTRAINT solver_assoc_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE; 
ALTER TABLE solver_assoc ADD CONSTRAINT solver_assoc_solver_id FOREIGN KEY (solver_id) REFERENCES solvers(id) ON DELETE CASCADE; 
ALTER TABLE community_requests ADD CONSTRAINT community_requests_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE; 
ALTER TABLE community_requests ADD CONSTRAINT community_requests_community FOREIGN KEY (community) REFERENCES spaces(id) ON DELETE CASCADE; 
ALTER TABLE pass_reset_request ADD CONSTRAINT pass_reset_request_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE; 
ALTER TABLE bench_dependency ADD CONSTRAINT bench_dependency_primary_bench_id FOREIGN KEY (primary_bench_id) REFERENCES benchmarks(id) ON DELETE CASCADE; 
ALTER TABLE bench_dependency ADD CONSTRAINT bench_dependency_secondary_bench_id FOREIGN KEY (secondary_bench_id) REFERENCES benchmarks(id) ON DELETE CASCADE; 
ALTER TABLE comments ADD CONSTRAINT comments_benchmark_id FOREIGN KEY (benchmark_id) REFERENCES benchmarks(id) ON DELETE CASCADE; 
ALTER TABLE comments ADD CONSTRAINT comments_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE; 
ALTER TABLE comments ADD CONSTRAINT comments_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE; 
ALTER TABLE comments ADD CONSTRAINT comments_solver_id FOREIGN KEY (solver_id) REFERENCES solvers(id) ON DELETE CASCADE; 
ALTER TABLE space_default_settings ADD CONSTRAINT space_default_settings_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE; 
ALTER TABLE space_default_settings ADD CONSTRAINT space_default_settings_post_processor FOREIGN KEY (post_processor) REFERENCES processors(id) ON DELETE SET NULL; 
ALTER TABLE space_default_settings ADD CONSTRAINT space_default_settings_default_benchmark FOREIGN KEY (default_benchmark) REFERENCES benchmarks(id) ON DELETE SET NULL; 
ALTER TABLE job_space_assoc ADD CONSTRAINT job_space_assoc_space_id FOREIGN KEY (space_id) REFERENCES job_spaces(id) ON DELETE CASCADE; 
ALTER TABLE job_space_assoc ADD CONSTRAINT job_space_assoc_child_id FOREIGN KEY (child_id) REFERENCES job_spaces(id) ON DELETE CASCADE; 
ALTER TABLE job_stats ADD CONSTRAINT job_stats_job_space_id FOREIGN KEY (job_space_id) REFERENCES job_spaces(id) ON DELETE CASCADE; 
ALTER TABLE benchmark_uploads ADD CONSTRAINT benchmark_uploads_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE;
ALTER TABLE benchmark_uploads ADD CONSTRAINT benchmark_uploads_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
