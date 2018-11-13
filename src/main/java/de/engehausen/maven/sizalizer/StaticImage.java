package de.engehausen.maven.sizalizer;

public class StaticImage {

	/** whether or not to only render a static image */
	public boolean only;
	public int width;
	public int height;
	public int depth;
	public int fontSize;

	public StaticImage() {
		this(false, 640, 480, 16, 12);
	}
	
	public StaticImage(final boolean only, final int width, final int height, final int depth, final int fontSize) {
		this.only = only;
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.fontSize = fontSize;
	}
}
