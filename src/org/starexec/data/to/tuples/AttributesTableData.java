package org.starexec.data.to.tuples;

import java.util.Objects;

/**
 * Created by agieg on 9/15/2016.
 */
public class AttributesTableData {

    public final Integer solverId;
    public final String solverName;
    public final Integer configId;
    public final String configName;
    public final String attrValue;
    public final Integer attrCount;
    public final Double wallclockSum;
    public final Double cpuSum;

    public AttributesTableData(Integer solverId, String solverName, Integer configId, String configName,
                               String attrValue, Integer attrCount, Double wallclockSum, Double cpuSum) {
        this.solverId = solverId;
        this.solverName = solverName;
        this.configId = configId;
        this.configName = configName;
        this.attrValue = attrValue;
        this.attrCount = attrCount;
        this.wallclockSum = wallclockSum;
        this.cpuSum = cpuSum;
    }

    // Getters are necessary if we want to use this tuple in JSP.

    public Integer getSolverId() {
        return solverId;
    }

    public String getSolverName() {
        return solverName;
    }


    public Integer getConfigId() {
        return configId;
    }

    public String getConfigName() {
        return configName;
    }

    public Integer getAttrCount() {
        return attrCount;
    }

    public String getAttrValue() {
        return attrValue;
    }


    @Override
    public int hashCode() {
        return Objects.hash(solverId, solverName, configId, configName, attrCount, attrValue);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof AttributesTableData)) {
            return false;
        } else if (object == this) {
            return true;
        } else {
            AttributesTableData atd = (AttributesTableData)object;
            return (this.solverId.equals(atd.solverId)
                    && this.solverName.equals(atd.solverName)
                    && this.configId.equals(atd.configId)
                    && this.configName.equals(atd.configName)
                    && this.attrCount.equals(atd.attrCount)
                    &&  this.attrValue.equals(atd.attrValue) );
        }
    }

}
