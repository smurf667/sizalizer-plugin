package de.engehausen.maven.sizalizer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class ZipAnalyzer implements Analyzer {
	
	private static final Set<String> SUPPORTED_PACKAGINGS;
	
	static {
		SUPPORTED_PACKAGINGS = Arrays.asList(
			"jar",
			"zip",
			"war",
			"ear",
			"maven-plugin"
		)
		.stream()
		.collect(Collectors.toSet());
	}

	@Override
	public Set<String> supportedPackagings() {
		return SUPPORTED_PACKAGINGS;
	}

	@Override
	public Node analyze(final MavenProject project, final Log logger) {
		final File file = find(new File(project.getBuild().getDirectory()), project.getBuild().getFinalName(), logger);
		return readZip(file, logger);
	}
	
	protected File find(final File directory, final String name, final Log logger) {
		final List<File> candidates = new ArrayList<>(3);
		for (final File candidate : directory.listFiles( f -> f.isFile() && f.getName().startsWith(name))) {
			candidates.add(candidate);
		}
		if (candidates.isEmpty()) {
			logger.warn("Cannot find " + name + "* in " + directory.getAbsolutePath());
			return null;
		} else if (candidates.size() == 1) {
			return candidates.get(0);
		}
		candidates.sort((a, b) -> b.getName().compareTo(a.getName()));
		return candidates.get(0);
		
	}
	
	protected boolean supportedSuffix(final File file) {
		final String name = file.getName();
		final int idx = name.lastIndexOf('.');
		if (idx > 0) {
			return SUPPORTED_PACKAGINGS.contains(name.substring(idx));
		}
		return false;
	}
	
	protected Node readZip(final File in, final Log logger) {
		final Node result = new Node();
		result.label = "/";
		if (in != null) {
			try (final ZipFile zipFile = new ZipFile(in)) {
				final Enumeration<? extends ZipEntry> entries = zipFile.entries();
				while (entries.hasMoreElements()) {
					final ZipEntry zipEntry = entries.nextElement();
					if (!zipEntry.isDirectory()) {
						addNode(result, zipEntry.getName().split("/"), 0, zipEntry.getSize());
					}
				}
			} catch (IOException e) {
				logger.error("Error reading " + in.getAbsolutePath(), e);
			}
		}
		return result;
	}

	private void addNode(final Node parent, final String[] path, final int pos, final long size) {
		if (pos < path.length - 1) {
			if (parent.children == null) {
				parent.children = new ArrayList<>();
			}
			final String label = path[pos];
			for (final Node candidate : parent.children) {
				if (label.equals(candidate.label)) {
					addNode(candidate, path, pos + 1, size);
					return;
				}
			}
			final Node child = new Node(parent);
			child.label = label;
			child.weight = 0;
			parent.addChild(child);
			addNode(child, path, pos + 1, size);
		} else {
			// this is the end
			final Node child = new Node(parent);
			child.weight = size;
			child.label = path[pos];
			parent.addChild(child);
		}
	}

}
