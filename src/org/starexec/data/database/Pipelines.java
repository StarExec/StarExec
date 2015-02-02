package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.starexec.data.to.pipelines.*;
import org.starexec.data.to.pipelines.PipelineDependency.PipelineDependencyType;

/**
 * Class responsible for inserting and removing pipelines from the database
 * @author Eric
 *
 */
public class Pipelines {
	private static final Logger log=Logger.getLogger(Pipelines.class);
	
	/**
	 * Returns a list of dependencies that go with the pipeline stage with the given ID
	 * @param stageId The stage to get dependencies before
	 * @param con An open SQL connection to make the call on 
	 * @return A list of all the dependencies for the given stage
	 */
	public static List<PipelineDependency> getDependenciesForStage(int stageId, Connection con) {
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			procedure=con.prepareCall("{CALL GetDependenciesForPipelineStage(?)}");
			procedure.setInt(1,stageId);
			results=procedure.executeQuery();
			List<PipelineDependency> answers=new ArrayList<PipelineDependency>();
			while (results.next()) {
				PipelineDependency dep=new PipelineDependency();
				dep.setStageId(stageId);
				dep.setDependencyId(results.getInt("dependency_id"));
				dep.setType(PipelineDependencyType.valueOf(results.getInt("dependency_type")));
				answers.add(dep);
			}
			return answers;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	/**
	 * Retrieves a list of stages for the given pipeline. Dependencies ARE populated
	 * @param pipeId The ID of the solver pipeline to get stages before
	 * @param con An open SQL connection to make the call on
	 * @return The list of pipeline stages
	 */
	public static List<PipelineStage> getStagesForPipeline(int pipeId, Connection con) {
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			procedure=con.prepareCall("{CALL GetStagesByPipelineId(?)}");
			procedure.setInt(1,pipeId);
			results=procedure.executeQuery();
			List<PipelineStage> stages=new ArrayList<PipelineStage>();
			while (results.next()) {
				PipelineStage stage=new PipelineStage();
				stage.setPipelineId(pipeId);
				stage.setExecutableId(results.getInt("executable_id"));
				stage.setId(results.getInt("stage_id"));
				stage.setDependencies(getDependenciesForStage(stage.getId(),con));
				stages.add(stage);
			}
			return stages;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}  finally {
			
		}
		return null;
	}
	
	/**
	 * Gets a solver pipeline from the database, including all the pipeline's stages
	 * and dependencies
	 * @param id
	 * @return
	 */
	public static SolverPipeline getFullPipeline(int id) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetPipelineById(?)}");
			procedure.setInt(1,id);
			results=procedure.executeQuery();
			if (results.next()) {
				SolverPipeline pipe=new SolverPipeline();
				pipe.setId(id);
				pipe.setName(results.getString("name"));
				pipe.setUploadDate(results.getTimestamp("uploaded"));
				pipe.setUserId(results.getInt("user_id"));
				pipe.setStages(getStagesForPipeline(id,con));
				return pipe;
			}
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	/**
	 * Adds the given pipeline dependency to the database. The stage associated with this
	 * dependency must already exist
	 * @param dep The dependency to add
	 * @param con An open SQL connection to make the call on 
	 * @return True on success and false on error
	 */
	public static boolean addDependencyToDatabase(PipelineDependency dep, Connection con) {
		CallableStatement procedure=null;
		try {
			procedure=con.prepareCall("{CALL AddPipelineDependency(?,?,?)}");
			procedure.setInt(1,dep.getStageId());
			procedure.setInt(2,dep.getDependencyId());
			procedure.setInt(3,dep.getType().getVal());
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}
	
	/**
	 * Adds a solver pipeline stage to the database, including adding all 
	 * dependencies present in the object
	 * @param stage A fully populated solver pipeline object, including dependencies
	 * @param con An open SQL connection to make this call on
	 * @return The ID of the pipeline object, or -1 on failure. The ID will also be set
	 * in the given pipeline object on success. All stage IDs will also be set
	 */
	public static int addPipelineStageToDatabase(PipelineStage stage, Connection con) {
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL AddPipelineStage(?,?,?)}");
			procedure.setInt(1, stage.getPipelineId());
			procedure.setInt(2,stage.getExecutableId());
			procedure.registerOutParameter(3, java.sql.Types.INTEGER);
			procedure.executeUpdate();
			int id = procedure.getInt(3);			
			stage.setId(id);
			
			for (PipelineDependency dep : stage.getDependencies()) {
				dep.setStageId(stage.getId());
				addDependencyToDatabase(dep,con);
			}
			
			return id;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		
		return -1;
	}
	
	/**
	 * Adds a solver pipeline to the database, including adding all stages and
	 * dependencies present in the object
	 * @param pipe A fully populated solver pipeline object, including dependencies
	 * @return The ID of the pipeline object, or -1 on failure. The ID will also be set
	 * in the given pipeline object on success. All stage IDs will also be set
	 */
	public static int addPipelineToDatabase(SolverPipeline pipe) {
		Connection con=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL AddPipeline(?,?,?)}");
			procedure.setInt(1, pipe.getUserId());
			procedure.setString(2,pipe.getName());
			procedure.registerOutParameter(3, java.sql.Types.INTEGER);
			procedure.executeUpdate();
			int id = procedure.getInt(3);			
			pipe.setId(id);
			
			for (PipelineStage stage : pipe.getStages()) {
				stage.setPipelineId(pipe.getId());
				addPipelineStageToDatabase(stage,con);
			}
			
			
			return id;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return -1;
	}
}
