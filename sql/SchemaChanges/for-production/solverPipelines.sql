USE starexec;

ALTER TABLE solvers ADD COLUMN executable_type INT DEFAULT 1;

-- table for storing the top level of solver pipelines
CREATE TABLE solver_pipelines (
	id INT NOT NULL AUTO_INCREMENT,
	name VARCHAR(128),
	user_id INT NOT NULL,
	uploaded TIMESTAMP NOT NULL,

	PRIMARY KEY(id)
);

CREATE TABLE pipeline_stages (
	stage_id INT NOT NULL AUTO_INCREMENT, -- orders the stages of this pipeline
	pipeline_id INT NOT NULL,
	executable_id INT NOT NULL,
	PRIMARY KEY (stage_id), -- pipelines can have many stages
	CONSTRAINT pipeline_stages_pipeline_id FOREIGN KEY (pipeline_id) REFERENCES solver_pipelines(id) ON DELETE CASCADE,
	CONSTRAINT pipeline_stages_solver_id FOREIGN KEY (executable_id) REFERENCES solvers(id) ON DELETE CASCADE
);
-- Stores any dependencies that pipeline stages have on benchmarks or previous artifacts
CREATE TABLE pipeline_dependencies (
	stage_id INT NOT NULL,
	dependency_id INT NOT NULL, -- id of either the benchmark or pipeline stage that is a dependency
	dependency_type INT NOT NULL, -- type of the dependency (which is either a benchmark or previous artifact)
	PRIMARY KEY (stage_id, dependency_id,dependency_type),
	CONSTRAINT pipeline_dependencies_stage_id FOREIGN KEY (stage_id) REFERENCES pipeline_stages(stage_id) ON DELETE CASCADE
);