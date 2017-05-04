package org.starexec.data.security;

import org.starexec.data.database.Uploads;
import org.starexec.data.to.BenchmarkUploadStatus;
/**
 * Determines whether users have authorization to view BenchmarkUploadStatus data
 *
 */
public class UploadSecurity {

	/**
	 * Determines whether a user can see a BenchmarkUploadStatus object that owns the given unvalidated
	 * benchmark
	 * @param userId The ID of the user making the request
	 * @param unvalidatedBenchmarkId The ID of the unvalidated benchmark
	 * @return A ValidatorStatusCode ojbect
	 */
	public static ValidatorStatusCode canViewUnvalidatedBenchmarkOutput(int userId, int unvalidatedBenchmarkId) {
		if (GeneralSecurity.hasAdminReadPrivileges(userId)) {
			return new ValidatorStatusCode(true);
		}
		BenchmarkUploadStatus status = Uploads.getUploadStatusForInvalidBenchmarkId(unvalidatedBenchmarkId);
		if (status!=null && status.getUserId()!=userId) {
			return new ValidatorStatusCode(false, "You may only view your own benchmark uploads");
		}
		return new ValidatorStatusCode(true);
	}
}
