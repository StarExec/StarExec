package org.starexec.data.database;

import org.starexec.data.to.pipelines.PipelineDependency;
import org.starexec.data.to.pipelines.PipelineDependency.PipelineInputType;
import org.starexec.data.to.pipelines.PipelineStage;
import org.starexec.data.to.pipelines.SolverPipeline;
import org.starexec.logger.StarLogger;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class responsible for inserting and removing pipelines from the database
 *
 * @author Eric
 */
public class Pipelines {
	private static final StarLogger log = StarLogger.getLogger(Pipelines.class);

	/**
	 * Returns a list of dependencies that go with the pipeline stage with the given ID. Dependencies will be returned
	 * in order of their input_number.
	 *
	 * @param stageId The ID of a pipeline_stage
	 * @param con An open SQL connection to make the call on
	 * @return A list of all the dependencies for the given stage
	 */
	public static List<PipelineDependency> getDependenciesForStage(int stageId, Connection con) {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL GetDependenciesForPipelineStage(?)}");
			procedure.setInt(1, stageId);
			results = procedure.executeQuery();
			List<PipelineDependency> answers = new ArrayList<>();
			while (results.next()) {
				PipelineDependency dep = new PipelineDependency();
				dep.setStageId(stageId);
				dep.setDependencyId(results.getInt("input_id"));
				dep.setType(PipelineInputType.valueOf(results.getInt("input_type")));
				dep.setInputNumber(results.getInt("input_number"));
				answers.add(dep);
			}
			return answers;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Returns all the dependencies that are associated with the given job pair, organized by stage number.
	 * Dependencies
	 * in each list are ordered by input number
	 *
	 * @param pairId The pair to get dependencies for.
	 * @param con The open connection make the call on.
	 * @return A HashMap that maps stage numbers to lists of pipeline dependencies.
	 */
	public static HashMap<Integer, List<PipelineDependency>> getDependenciesForJobPair(int pairId, Connection con) {
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			procedure = con.prepareCall("{CALL GetDependenciesForJobPair(?)}");
			procedure.setInt(1, pairId);
			results = procedure.executeQuery();
			HashMap<Integer, List<PipelineDependency>> answers = new HashMap<>();
			while (results.next()) {
				PipelineDependency dep = new PipelineDependency();
				dep.setStageId(results.getInt("stage_id"));
				dep.setDependencyId(results.getInt("input_id"));
				dep.setType(PipelineInputType.valueOf(results.getInt("input_type")));
				dep.setInputNumber(results.getInt("input_number"));

				if (!answers.containsKey(dep.getStageId())) {
					answers.put(dep.getStageId(), new ArrayList<>());
				}

				answers.get(dep.getStageId()).add(dep);
			}
			return answers;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Retrieves a list of stages for the given pipeline. Dependencies ARE populated
	 *
	 * @param pipeId The ID of the solver pipeline to get stages before
	 * @param con An open SQL connection to make the call on
	 * @return The list of pipeline stages
	 */
	public static List<PipelineStage> getStagesForPipeline(int pipeId, Connection con) {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL GetStagesByPipelineId(?)}");
			procedure.setInt(1, pipeId);
			results = procedure.executeQuery();
			List<PipelineStage> stages = new ArrayList<>();
			while (results.next()) {
				PipelineStage stage = new PipelineStage();
				stage.setPipelineId(pipeId);
				stage.setConfigId(results.getInt("config_id"));
				stage.setId(results.getInt("stage_id"));
				stage.setNoOp(results.getBoolean("is_noop"));
				stage.setDependencies(getDependenciesForStage(stage.getId(), con));
				stages.add(stage);
			}
			return stages;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Gets a solver pipeline from the database, including all the pipeline's stages and dependencies
	 *
	 * @param id The ID of the pipeline
	 * @return The pipeline, with everything populated, or null on error
	 */
	public static SolverPipeline getFullPipeline(int id) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetPipelineById(?)}");
			procedure.setInt(1, id);
			results = procedure.executeQuery();
			if (results.next()) {
				SolverPipeline pipe = new SolverPipeline();
				pipe.setId(id);
				pipe.setName(results.getString("name"));
				pipe.setUploadDate(results.getTimestamp("uploaded"));
				pipe.setUserId(results.getInt("user_id"));
				pipe.setPrimaryStageNumber(results.getInt("primary_stage_id"));
				pipe.setStages(getStagesForPipeline(id, con));
				return pipe;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Adds the given pipeline dependency to the database. The stage associated with this dependency must already exist
	 *
	 * @param dep The dependency to add
	 * @param con An open SQL connection to make the call on
	 */
	public static void addDependencyToDatabase(PipelineDependency dep, Connection con) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL AddPipelineDependency(?,?,?,?)}");
			procedure.setInt(1, dep.getStageId());
			procedure.setInt(2, dep.getDependencyId());
			log.debug("adding dependency with type " + dep.getType());
			procedure.setInt(3, dep.getType().getVal());
			procedure.setInt(4, dep.getInputNumber());
			procedure.executeUpdate();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(procedure);
		}
	}

	/**
	 * Adds a solver pipeline stage to the database, including adding all dependencies present in the object
	 *
	 * @param stage A fully populated solver pipeline object, including dependencies
	 * @param con An open SQL connection to make this call on
	 */
	public static void addPipelineStageToDatabase(PipelineStage stage, Connection con) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL AddPipelineStage(?,?,?,?,?)}");
			procedure.setInt(1, stage.getPipelineId());
			if (stage.isNoOp()) {
				procedure.setNull(2, java.sql.Types.INTEGER);
			} else {
				procedure.setInt(2, stage.getConfigId());
			}
			procedure.setBoolean(3, stage.isPrimary());
			procedure.setBoolean(4, stage.isNoOp());
			procedure.registerOutParameter(5, java.sql.Types.INTEGER);
			log.debug("trying to use the config id = " + stage.getConfigId());
			procedure.executeUpdate();
			int id = procedure.getInt(5);
			stage.setId(id);

			for (PipelineDependency dep : stage.getDependencies()) {
				dep.setStageId(stage.getId());
				addDependencyToDatabase(dep, con);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(procedure);
		}
	}

	/**
	 * Adds a solver pipeline to the database, including adding all stages and dependencies present in the object
	 *
	 * @param pipe A fully populated solver pipeline object, including dependencies
	 * @return The ID of the pipeline object, or -1 on failure. The ID will also be set in the given pipeline object on
	 * success. All stage IDs will also be set
	 */
	public static int addPipelineToDatabase(SolverPipeline pipe) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL AddPipeline(?,?,?)}");
			procedure.setInt(1, pipe.getUserId());
			procedure.setString(2, pipe.getName());
			procedure.registerOutParameter(3, java.sql.Types.INTEGER);
			procedure.executeUpdate();
			int id = procedure.getInt(3);
			pipe.setId(id);

			int number = 1;
			for (PipelineStage stage : pipe.getStages()) {
				stage.setPipelineId(pipe.getId());
				if (number == pipe.getPrimaryStageNumber()) {
					stage.setPrimary(true);
				} else {
					stage.setPrimary(false);
				}
				addPipelineStageToDatabase(stage, con);
				number++;
			}


			return id;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}

		return -1;
	}

	/**
	 * Returns all of the solver pipelines that are used in the given job.
	 *
	 * @param jobId The ID of the job to get all pipelines for
	 * @return A list of Solver Pipelines that are referenced by the job, or null on error
	 */
	public static List<SolverPipeline> getPipelinesByJob(int jobId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		List<Integer> pipeIds = new ArrayList<>();
		List<SolverPipeline> pipes = new ArrayList<>();
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("CALL GetPipelineIdsByJob(?)");
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();
			while (results.next()) {
				pipeIds.add(results.getInt("id"));
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		for (Integer i : pipeIds) {
			pipes.add(Pipelines.getFullPipeline(i));
		}

		return pipes;
	}

	/**
	 * Deletes a pipeline from the database, including deletion of all stages and pipeline_dependencies entries
	 *
	 * @param pipelineId The ID of the pipeline being deleted
	 */
	public static void deletePipelineFromDatabase(int pipelineId) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL DeletePipeline(?)}");
			procedure.setInt(1, pipelineId);
			procedure.executeUpdate();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}
}
