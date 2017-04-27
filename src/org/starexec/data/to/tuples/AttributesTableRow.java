package org.starexec.data.to.tuples;

import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;

public class AttributesTableRow {
    public int solverId;
    public String solverName;
    public int configId;
    public String configName;
    public List<Triple<Integer, String, String>> countAndTimes = new ArrayList<>();

    public int getSolverId() {
        return solverId;
    }

    public void setSolverId(int solverId) {
        this.solverId = solverId;
    }

    public String getSolverName() {
        return solverName;
    }

    public void setSolverName(String solverName) {
        this.solverName = solverName;
    }

    public int getConfigId() {
        return configId;
    }

    public void setConfigId(int configId) {
        this.configId = configId;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public List<Triple<Integer, String, String>> getCountAndTimes() {
        return countAndTimes;
    }

    public void setCountAndTimes(List<Triple<Integer, String, String>> countAndTimes) {
        this.countAndTimes = countAndTimes;
    }
}
