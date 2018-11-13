package de.engehausen.maven.sizalizer;

public class Mapping {

	/** ECMA script regular expression for matching */
	public String match;
	/** the color to use in case of a match */
	public String rgb;

	public Mapping() {}
	
	public Mapping(final String match, final String rgb) {
		this.match = match;
		this.rgb = rgb;
	}
}
