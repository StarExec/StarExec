package org.starexec.data.to;

/**
 * Represents a Syntax highlighter, used to highlight Benchmarks
 */
public class Syntax extends Identifiable {
	public final String name;
	public final String classname;
	public final String js;

	public Syntax(int id, String name, String classname, String js) {
		setId(id);
		this.name = name;
		this.classname = classname;
		this.js = js;
	}
}
