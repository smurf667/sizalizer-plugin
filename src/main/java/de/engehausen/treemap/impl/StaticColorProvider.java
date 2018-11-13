package de.engehausen.treemap.impl;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.engehausen.maven.sizalizer.Mapping;
import de.engehausen.maven.sizalizer.Node;
import de.engehausen.treemap.IColorProvider;
import de.engehausen.treemap.IRectangle;
import de.engehausen.treemap.ITreeModel;

public class StaticColorProvider implements IColorProvider<Node, Color> {

	private static Color DEFAULT_COLOR = Color.RED;
	private final Map<Pattern, Color> colors;
	
	public StaticColorProvider(final Mapping[] mappings) {
		colors = new HashMap<>();
		for (final Mapping mapping : mappings) {
			colors.put(Pattern.compile(mapping.match), toColor(mapping.rgb));
		}
	}

	@Override
	public Color getColor(final ITreeModel<IRectangle<Node>> model, final IRectangle<Node> rectangle) {
		final String name = rectangle.getNode().label;
		for (Map.Entry<Pattern, Color> entry : colors.entrySet()) {
			final Matcher matcher = entry.getKey().matcher(name);
			if (matcher.find()) {
				return entry.getValue();
			}
		}
		return DEFAULT_COLOR;
	}

	private Color toColor(final String rgb) {
		Color result = DEFAULT_COLOR;
		if (rgb != null) {
			if (rgb.length() == 4) {
				result = split(rgb.substring(1), 1, s -> Integer.valueOf(16 * Integer.parseInt(s, 16)));
			} else if (rgb.length() == 7) {
				result = split(rgb.substring(1), 2, s -> Integer.valueOf(Integer.parseInt(s, 16)));
			}
		}
		return result;
	}

	private Color split(final String hex, final int size, final Function<String, Integer> converter) {
		final int[] rgb = new int[3];
		for (int i = 0, j = 0; j < rgb.length; i += size, j++) {
			final Integer value = converter.apply(hex.substring(i, i + size));
			rgb[j] = value.intValue();
		}
		return new Color(rgb[0], rgb[1], rgb[2]);
	}

}
