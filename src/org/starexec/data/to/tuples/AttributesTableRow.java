package org.starexec.data.to.tuples;

import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Solver;
import org.apache.commons.lang3.tuple.Triple;


import java.util.ArrayList;
import java.util.List;

public class AttributesTableRow {
    public int solverId;
    public String solverName;
    public int configId;
    public String configName;
    public List<Triple<Integer, Double, Double>> countAndTimes = new ArrayList<>();
}
