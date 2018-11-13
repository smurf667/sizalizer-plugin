package de.engehausen.maven.sizalizer;

import java.util.Set;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * Artifact analyzer interface.
 * Describes the packagings it supports and performs the analysis.
 */
public interface Analyzer {

	/**
	 * Returns the set of supported <a href="https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html#Packaging">packagings</a>.
	 * @return the set of supported packagings, never {@code null}.
	 */
	Set<String> supportedPackagings();

	/**
	 * Analyzes an artifact.
	 * @param project the Maven project for which to analyze an artifact, never {@code null}.
	 * @param logger the logger, never {@code null}.
	 * @return the root node of the tree, which is the analysis result - never {@code null}
	 */
	Node analyze(MavenProject project, Log logger);

}
