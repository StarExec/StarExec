package org.starexec.data.to.enums;

import org.starexec.util.Util;

/**
 * Created by agieg on 11/14/2016.
 */
public enum JobXmlType {

    STANDARD(Util.url("public/batchJobSchema.xsd")),
    SOLVER_UPLOAD(Util.url("public/runSolverOnUploadBatchJobSchema.xsd"));

    public final String schemaPath;
    JobXmlType(String schemaPath) {
        this.schemaPath = schemaPath;
    }
}