package org.starexec.data.to.enums;

import org.apache.log4j.Logger;
import org.starexec.util.LogUtil;
import org.starexec.util.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by agieg on 11/14/2016.
 */
public enum JobXmlType {

    STANDARD(Util.url("public/batchJobSchema.xsd")),
    SOLVER_UPLOAD(Util.url("public/runSolverOnUploadBatchJobSchema.xsd"));

    private static final Logger jobXmlTypeLog = Logger.getLogger(JobXmlType.class);
    private static final LogUtil jobXmlTypeLogUtil = new LogUtil(jobXmlTypeLog);
    public final String schemaPath;
    private Map<String, Integer> nameToNewPrimitiveId;
    JobXmlType(String schemaPath) {
        this.schemaPath = schemaPath;
        this.nameToNewPrimitiveId = new HashMap<>();
    }

    /**
     * This method is used to add name to id mappings to the nameToNewPrimitiveId map.
     * This should ONLY be used with job xml types that are not STANDARD. STANDARD job xml types
     * should already have access to all primitive ids since they assume the primitives are already
     * in the database.
     * @param primitiveName the name of the primitive to set an id for.
     * @param primitiveId the id of the primitve to associate a name with.
     */
    public void addNameToIdMapping(final String primitiveName, final Integer primitiveId) {
        final String methodName = "addNameToIdMapping";

        if (this == JobXmlType.STANDARD) {
            jobXmlTypeLogUtil.error(methodName, "Attempt was made to set primitive id from a standard job XML upload. " +
                    "Throwing exception...");
            throw new IllegalStateException("You cannot set primitive IDs for standard job XML uploads."
                    + " The primitive IDs are specified in the ");
        }

        nameToNewPrimitiveId.put(primitiveName, primitiveId);
    }

    /**
     * This method is used to get name to id mappings in the nameToNewPrimitiveId map.
     * This should ONLY be used with job xml types that are not STANDARD. STANDARD job xml types
     * should already have access to all primitive ids since they assume the primitives are already
     * in the database.
     * @param name the name of the primitive to get the id for.
     * @return the id of the primitive.
     */
    public Integer getIdWithName(final String name) {
        final String methodName = "getIdWithName";

        if (this == JobXmlType.STANDARD) {
            jobXmlTypeLogUtil.error(methodName, "Attempt was made to get primitive id from a standard job XML upload. " +
                    "Throwing exception...");
            throw new IllegalStateException("You cannot get primitive IDs for standard job XML uploads."
                    + " The primitive IDs are specified in the ");
        }

        if (!nameToNewPrimitiveId.containsKey(name)) {
            jobXmlTypeLogUtil.error(methodName, "Attempt was made to get primitive id without it being set. Throwing exception...");
            throw new IllegalArgumentException("The name, "+name+" was never added to the map. Use instance method"
                    +" addNameToIdMapping(String, Integer) to do so.");
        }

        return nameToNewPrimitiveId.get(name);
    }
}