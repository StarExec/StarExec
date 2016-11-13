package org.starexec.data.to.tuples;

/**
 * Created by agieg on 11/13/2016.
 */
public class UploadSolverResult {
    public final UploadSolverStatus status;
    public final int solverId;
    public final boolean hadConfigs;
    public final boolean isBuildJob;

    public UploadSolverResult(UploadSolverStatus status, int solverId, boolean hadConfigs, boolean isBuildJob) {
        this.status = status;
        this.solverId = solverId;
        this.hadConfigs = hadConfigs;
        this.isBuildJob = isBuildJob;
    }

    public enum UploadSolverStatus {
        Success("Success"),
        DescriptionMalformed("The archive description file is malformed. Make sure it does not exceed 1024 characters."),
        CannotAccessFile("File could not be accessed at URL"),
        ExceedQuota("File is too large to fit in user's disk quota"),
        ExtractingError("Internal error when extracting solver");

        public final String message;
        UploadSolverStatus(String message) {
            this.message = message;
        }
    }
}
