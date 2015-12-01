package org.starexec.data.security;

import org.starexec.data.database.Uploads;
import org.starexec.data.database.Users;
import org.starexec.data.to.BenchmarkUploadStatus;

public class UploadSecurity {

	public static ValidatorStatusCode canViewUnvalidatedBenchmarkOutput(int userId, int unvalidatedBenchmarkId) {
		if (Users.isAdmin(userId)) {
			return new ValidatorStatusCode(true);
		}
		BenchmarkUploadStatus status = Uploads.getUploadStatusForInvalidBenchmarkId(unvalidatedBenchmarkId);
		if (status!=null && status.getUserId()!=userId) {
			return new ValidatorStatusCode(false, "You may only view your own benchmark uploads");
		}
		return new ValidatorStatusCode(true);
	}
}
