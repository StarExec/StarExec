package org.starexec.data.to.tuples;

import java.util.Optional;

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
        SUCCESS("Success"),
        SUCCESS_TEST_FAILED("Solver uploaded successfully but your test job could not be created."),
        DESCRIPTION_MALFORMED("The archive description file is malformed. Make sure it does not exceed 1024 characters."),
        CANNOT_ACCESS_FILE("File could not be accessed at URL"),
        EXCEED_QUOTA("File is too large to fit in user's disk quota"),
        EXTRACTING_ERROR("Internal error when extracting solver");

        public final String message;
        public Optional<String> optionalMessage = Optional.empty();
        UploadSolverStatus(String message) {
            this.message = message;
        }
    }
}
