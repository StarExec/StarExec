package org.starexec.data.to.enums;


public enum Primitive {
    JOB("jobType"),
    USER("userType"),
    SOLVER("solverType"),
    BENCHMARK("benchmarkType"),
    SPACE("spaceType"),
    JOB_PAIR("jobPairType"),
    JOB_STATS("jobStatsType"),
    NODE("nodeType"),
    QUEUE("queueType"),
    CONFIGURATION("configurationType");

    // The name of the css class of the hidden span that contains the name of the type.
    final String cssClass;
    Primitive(String cssClass) {
        this.cssClass = cssClass;
    }
}
