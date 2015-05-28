package org.starexec.util.matrixView;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.to.Job;


public class MatrixViewUtil {

	public static Job getJobIfAvailableToUser(int jobId, int userId, HttpServletResponse response) throws IOException {
		if(Permissions.canUserSeeJob(jobId,userId)) {
			Job job = Jobs.get(jobId);
			
			int jobSpaceId=job.getPrimarySpace();
			// this means it's an old job and we should run the backwards-compatibility routine
			// to get everything set up first
			if (jobSpaceId == 0) {
				jobSpaceId = Jobs.setupJobSpaces(jobId);
			}
			
			if (jobSpaceId>0) {
				job = Jobs.get(jobId);
			} else {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "The details for this job could not be obtained");
				return null;
			}

			return job;
		} else {
			if (Jobs.isJobDeleted(jobId)) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "This job has been deleted. You likely want to remove it from your spaces");
				return null;
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job does not exist or is restricted");
				return null;
			}
		}
	}
}
