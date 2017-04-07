package org.starexec.data.to.enums;


import org.starexec.constants.R;

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
    public final String cssClass;
    Primitive(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getCssClass() {
        return cssClass;
    }

    /**
     * Method that maps our R string constants for primitives to this enum. Uses valueOf by default.
     * @param name the name of the type of the primitive.
     * @return the enum for the primitive or throw IllegalArgumentException if it doesn't exist.
     * @throws IllegalArgumentException
     */
    public static Primitive getEnum(String name) {
        switch (name) {
            case R.BENCHMARK:
                return BENCHMARK;
            case R.SOLVER:
                return SOLVER;
            case R.JOB:
                return JOB;
            case R.USER:
                return USER;
            case R.SPACE:
                return SPACE;
            case R.CONFIGURATION:
                return CONFIGURATION;
            default:
                return Primitive.valueOf(name);
        }
    }
}
