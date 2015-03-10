DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement


-- Gets data from the solver_pipelines table for the given id
DROP PROCEDURE IF EXISTS GetPipelineById;
CREATE PROCEDURE GetPipelineById(IN _id INT)
	BEGIN
		SELECT * FROM solver_pipelines WHERE id=_id;
	END //

-- Gets all the stage information from the pipeline_stages table for the given pipeline
DROP PROCEDURE IF EXISTS GetStagesByPipelineId;
CREATE PROCEDURE GetStagesByPipelineId(IN _id INT)
	BEGIN
		SELECT * FROM pipeline_stages WHERE pipeline_id=_id;
	END //
	
-- Given a stage ID, gets all the dependencies for the stage
DROP PROCEDURE IF EXISTS GetDependenciesForPipelineStage;
CREATE PROCEDURE GetDependenciesForPipelineStage(IN _id INT)
	BEGIN
		SELECT * FROM pipeline_dependencies WHERE stage_id=_id;
	END //
	
-- Adds a solver pipeline to the database
DROP PROCEDURE IF EXISTS AddPipeline;
CREATE PROCEDURE AddPipeline(IN _uid INT, IN _name VARCHAR(128), OUT _id INT)
	BEGIN
		INSERT INTO solver_pipelines (user_id, name, uploaded) VALUES (_uid, _name, NOW());
		
		SELECT LAST_INSERT_ID() INTO _id;

	END //

-- adds a solver pipeline stage for an existing pipeline to the database.
-- pipelines must be added to the database in the order that they are to be used in the pipeline
-- to ensure that the AUTO_INCREMENT IDs are ordered
DROP PROCEDURE IF EXISTS AddPipelineStage;
CREATE PROCEDURE AddPipelineStage(IN _pid INT, IN _cid INT, IN _keep BOOLEAN, IN _primary INT, OUT _id INT)
	BEGIN
		INSERT INTO pipeline_stages (pipeline_id, config_id, keep_output)
		VALUES (_pid, _cid,_keep);
		
		SELECT LAST_INSERT_ID() INTO _id;
		IF _primary THEN
			UPDATE solver_pipelines SET primary_stage_id = _id WHERE solver_pipelines.id = _pid;
		END IF;
	END //

-- Adds a dependency for an existing stage. 
DROP PROCEDURE IF EXISTS AddPipelineDependency;
CREATE PROCEDURE AddPipelineDependency(IN _sid INT, IN _iid INT, IN _type INT, IN _num INT)
	BEGIN
		INSERT INTO pipeline_dependencies (stage_id, input_id, input_type, input_number) VALUES (_sid, _iid,_type, _num);

	END //
	
-- deletes a pipeline from the database. This will also delete all of its dependencies and stages
DROP PROCEDURE IF EXISTS DeletePipeline;
CREATE PROCEDURE DeletePipeline(IN _pid INT)
	BEGIN
		DELETE FROM solver_pipelines WHERE id=_pid;
	END //
	
-- Gets all the pipeline IDs of pipelines referenced by the given job
DROP PROCEDURE IF EXISTS GetPipelineIdsByJob;
CREATE PROCEDURE GetPipelineIdsByJob(IN _jid INT)
	BEGIN
		SELECT DISTINCT solver_pipelines.id
		FROM job_pairs
		JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id=job_pairs.id
		JOIN pipeline_stages ON pipeline_stages.stage_id = jobpair_stage_data.stage_id
		JOIN solver_pipelines ON solver_pipelines.id = pipeline_stages.pipeline_id
		WHERE job_id=_jid;
	END //

DELIMITER ; -- This should always be at the end of this file