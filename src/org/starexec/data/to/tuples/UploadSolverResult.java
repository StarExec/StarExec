package org.starexec.data.to.tuples;

import java.util.Optional;

// Describes the result of a solver upload.
public class UploadSolverResult {
    public final UploadSolverStatus status;
    public final int solverId;
    public final boolean hadConfigs;
    public final boolean isBuildJob;
    public Optional<String> optionalMessage;

    public UploadSolverResult(
            UploadSolverStatus status,
            int solverId,
            boolean hadConfigs,
            boolean isBuildJob) {
        this.status = status;
        this.solverId = solverId;
        this.hadConfigs = hadConfigs;
        this.isBuildJob = isBuildJob;
        this.optionalMessage = Optional.empty();
    }

    public enum UploadSolverStatus {
        SUCCESS("Success"),
        DESCRIPTION_MALFORMED("The archive description file is malformed. Make sure it does not exceed 1024 characters or contain any illegal characters."),
        CANNOT_ACCESS_FILE("File could not be accessed at URL"),
        EXCEED_QUOTA("File is too large to fit in user's disk quota"),
        EXTRACTING_ERROR("Internal error when extracting solver");

        public final String message;
        UploadSolverStatus(String message) {
            this.message = message;
        }
    }
}
