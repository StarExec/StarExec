package org.starexec.data.to.enums;

import org.starexec.constants.R;

public enum Primitive {
    JOB("jobPrimitiveType"),UPLOAD("uploadPrimitiveType"), USER("userPrimitiveType"), SOLVER("solverPrimitiveType"), BENCHMARK(
			"benchmarkPrimitiveType"), SPACE("spacePrimitiveType"), JOB_PAIR("jobPairPrimitiveType"), JOB_STATS(
			"jobStatsPrimitiveType"), NODE("nodePrimitiveType"), QUEUE("queuePrimitiveType"), CONFIGURATION(
			"configurationPrimitiveType"), PROCESSOR("processorPrimitiveType");

	// The name of the css class of the hidden span that contains the name of the type.
	// If the css class name changes then the name will need to be changed on the frontend too.
	public final String cssClass;

	Primitive(String cssClass) {
		this.cssClass = cssClass;
	}

	public String getCssClass() {
		return cssClass;
	}

	/**
	 * Method that maps our R string constants for primitives to this enum. Uses valueOf by default.
	 *
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
			case R.PROCESSOR:
				return PROCESSOR;
		        case R.UPLOAD:
			        return UPLOAD;
			default:
				return Primitive.valueOf(name);
		}
	}
}
