package org.starexec.data.to.tuples;

/**
 * Created by agieg on 9/15/2016.
 */
public class AttributesTableData {

    public final Integer solverId;
    public final Integer configId;
    public final Integer attrCount;
    public final String attrValue;

    public AttributesTableData(Integer solverId, Integer configId, Integer attrCount, String attrValue) {
        this.solverId = solverId;
        this.configId = configId;
        this.attrCount = attrCount;
        this.attrValue = attrValue;
    }

    // Getters are necessary if we want to use this tuple in JSP.

    public Integer getSolverId() {
        return solverId;
    }

    public Integer getConfigId() {
        return configId;
    }

    public Integer getAttrCount() {
        return attrCount;
    }

    public String getAttrValue() {
        return attrValue;
    }

}
