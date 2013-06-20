package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.starexec.data.to.Comment;

/**
 * Handles all database interaction for comments
 * @author Vivek Sardeshmukh
 */
public class Comments {
	private static final Logger log = Logger.getLogger(Comments.class);
	public static enum CommentType { BENCHMARK, SOLVER, SPACE };
	
	/**
	 * Adds a new comment associated with the specified primitive
	 * @param id The ID of the primitive the comment is associated with
	 * @param user_id user id of the commenter
	 * @param desc The actual comment 
	 * @param type Which type of primitive to associate the comment with (benchmark, space, solver)
	 * @return True if the operation was a success, false otherwise
	 * @author Vivek Sardeshmukh
	 */
	public static boolean add(long id, long user_id, String desc, CommentType type) {
		Connection con = null;			
		CallableStatement procedure = null;
		
		try {
			con = Common.getConnection();		
			
			switch(type) {
				case BENCHMARK:
					procedure = con.prepareCall("{CALL AddBenchmarkComment(?, ?, ?)}");
					break;
				case SPACE:
					procedure = con.prepareCall("{CALL AddSpaceComment(?, ?, ?)}");
					break;
				case SOLVER:
					procedure = con.prepareCall("{CALL AddSolverComment(?, ?, ?)}");
					break;			
				default:
					throw new Exception("Unhandled value for CommentType");				
			}			
			
			procedure.setLong(1, id);
			procedure.setLong(2, user_id);
			procedure.setString(3, desc);
			
			procedure.executeUpdate();					
			log.info("Added a new comment to a " + type.toString() + " whose id is " + id + "; the comment was made by user " + user_id + ".");
			return true;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
		log.warn("Failed to add the comment : "+ desc);
		return false;
	}
	
	/**
	 * Returns a list of comments associated with the given primitive based on its type
	 * @param id The id of the primitive to get comments for
	 * @param cmtType The type of primitive to get comments for (solver, benchmark or space)
	 * @return A list of comments associated with the primitive
	 * @author Vivek Sardeshmukh
	 */
	public static List<Comment> getAll(long id, CommentType cmtType) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;		
		try {
			con = Common.getConnection();
			
			switch(cmtType) {
				case BENCHMARK:
					procedure = con.prepareCall("{CALL GetCommentsByBenchmarkId(?)}");
					break;
				case SPACE:
					procedure = con.prepareCall("{CALL GetCommentsBySpaceId(?)}");
					break;
				case SOLVER:
					procedure = con.prepareCall("{CALL GetCommentsBySolverId(?)}");
					break;
				default:
					throw new Exception("Unhandled value for CommentType");
			}
			
			procedure.setLong(1, id);
			
			results = procedure.executeQuery();
			List<Comment> comments = new LinkedList<Comment>();
			
			while (results.next()) {
				Comment w = new Comment();
			
				w.setUserId(results.getLong("user_id"));
				w.setFirstName(results.getString("first_name"));
				w.setId(results.getInt("comments.id"));
				w.setLastName(results.getString("last_name"));
				w.setDescription(results.getString("cmt"));
				w.setUploadDate(results.getTimestamp("cmt_date"));
				comments.add(w);				
			}
			
			return comments;
			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
		    Common.safeClose(results);
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
		
		return null;
	}	
	
	/**
	 * Deletes the comment associated with the given comment ID.
	 * @param commentId the ID of the comment to delete
	 * @return True if the operation was a success, false otherwise
	 * @author Vivek Sardeshmukh
	 */
	public static boolean delete(long commentId) {
		Connection con = null;			
		CallableStatement procedure = null;			
		
		try {
			con = Common.getConnection();		
			
			procedure = con.prepareCall("{CALL DeleteComment(?)}");
			procedure.setLong(1, commentId);		
			procedure.executeUpdate();					
			log.info("Comment " + commentId +" deleted." );
			return true;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(con);
		}
		log.warn("unable to delete the comment : " + commentId);
		return false;
	}	
}