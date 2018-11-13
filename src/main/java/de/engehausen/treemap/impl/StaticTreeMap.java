package de.engehausen.treemap.impl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.util.Iterator;

import de.engehausen.maven.sizalizer.Node;
import de.engehausen.treemap.IColorProvider;
import de.engehausen.treemap.IGenericTreeMapLayout;
import de.engehausen.treemap.IGenericWeightedTreeModel;
import de.engehausen.treemap.ILabelProvider;
import de.engehausen.treemap.IRectangle;
import de.engehausen.treemap.IRectangleRenderer;
import de.engehausen.treemap.ITreeMapLayout;
import de.engehausen.treemap.ITreeModel;
import de.engehausen.treemap.IWeightedTreeModel;

/**
 */
public class StaticTreeMap {

	protected ITreeModel<Node> model;
	protected ITreeMapLayout<Node> layout;
	protected ITreeModel<IRectangle<Node>> rectangles;
	protected Node currentRoot;
	protected BufferedImage image;
	protected IRectangleRenderer<Node, Graphics2D, Color> renderer;
	protected ILabelProvider<Node> labelProvider;
	protected IColorProvider<Node, Color> colorProvider;
	protected GraphicsConfiguration gc;

	/**
	 * Sets the rectangle renderer the tree map will use. If no renderer
	 * is set, a default will be used.
	 * @param aRenderer the rectangle renderer, must not be <code>null</code>.
	 */
	public void setRectangleRenderer(final IRectangleRenderer<Node, Graphics2D, Color> aRenderer) {
		renderer = aRenderer;
	}

	/**
	 * Returns the rectangle renderer used by the tree map.
	 * @return the rectangle renderer used by the tree map.
	 */
	public IRectangleRenderer<Node, Graphics2D, Color> getRectangleRenderer() {
		return renderer;
	}

	/**
	 * Sets the label provider the tree map will use during rendering.
	 * If no provider is set no labels are displayed.
	 * @param aProvider the label provider; may be <code>null</code>.
	 */
	public void setLabelProvider(final ILabelProvider<Node> aProvider) {
		labelProvider = aProvider;
	}

	/**
	 * Returns the currently active label provider of this tree map.
	 * @return the currently active label provider of this tree map.
	 */
	public ILabelProvider<Node> getLabelProvider() {
		return labelProvider;
	}

	/**
	 * Sets the color provider the tree map will use during rendering.
	 * If no provider is set a default will be used.
	 * @param aProvider the color provider; may be <code>null</code>.
	 */
	public void setColorProvider(final IColorProvider<Node, Color> aProvider) {
		colorProvider = aProvider;
	}

	/**
	 * Returns the color provider the tree map uses.
	 * @return the color provider the tree map uses.
	 */
	public IColorProvider<Node, Color> getColorProvider() {
		return colorProvider;
	}

	/**
	 * Sets the model to use in this tree map.
	 * @param aModel the model to use; must not be <code>null</code>.
	 */
	public void setTreeModel(final IWeightedTreeModel<Node> aModel) {
		if (layout == null) {
			layout = new SquarifiedLayout<Node>(2);
		}
		model = aModel;
		currentRoot = aModel.getRoot();
		rectangles = null;
		image = null;
	}

	/**
	 * Sets the model to use in this tree map.
	 * @param aModel the model to use; must not be <code>null</code>.
	 */
	public <T extends Number> void setTreeModel(final IGenericWeightedTreeModel<Node, T> aModel) {
		if (layout == null) {
			layout = new GenericSquarifiedLayout<Node, T>(2);
		}
		model = aModel;
		currentRoot = aModel.getRoot();
		rectangles = null;
		image = null;
	}

	/**
	 * Returns the model currently being used by the tree map.
	 * @return the model currently being used by the tree map, may be <code>null</code>.
	 */
	public ITreeModel<Node> getCurrentTreeModel() {
		return model;
	}

	/**
	 * Sets the layout for the tree map. If this method is not called,
	 * a default squarified layout with maximum nesting level two is used
	 * @param aLayout the layout to use, must not be <code>null</code>
	 */
	public void setTreeMapLayout(final ITreeMapLayout<Node> aLayout) {
		layout = aLayout;
	}

	/**
	 * Renders the rectangles of the tree map.
	 * @param g the graphics to draw on
	 * @param rects the rectangles to render
	 */
	protected void render(final Graphics2D g, final ITreeModel<IRectangle<Node>> rects) {
		final IRectangle<Node> root = rects.getRoot();
		if (root != null) {
			final FIFO<IRectangle<Node>> queue = new FIFO<IRectangle<Node>>();
			queue.push(rects.getRoot());
			while (queue.notEmpty()) {
				final IRectangle<Node> node = queue.pull();
				render(g, rects, node);
				if (rects.hasChildren(node)) {
					for (Iterator<IRectangle<Node>> i = rects.getChildren(node); i.hasNext(); ) {
						queue.push(i.next());
					}
				}
			}
		}
	}

	protected void render(final Graphics2D g, final ITreeModel<IRectangle<Node>> rects, final IRectangle<Node> rect) {
		renderer.render(g, rects, rect, colorProvider, labelProvider);
	}

	/**
	 * Rebuilds the image buffer used for rendering the tree map quickly.
	 * @param width the new width
	 * @param height the new height
	 * @param rects the rectangle model to use for rendering into the buffer
	 * @return the render result.
	 */
	protected BufferedImage rebuildImage(final int width, final int height, final ITreeModel<IRectangle<Node>> rects) {
		if (width*height > 0) {
			final BufferedImage result;
			if (GraphicsEnvironment.isHeadless()) {
				result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			} else {
				if (gc == null) {
					final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
					final GraphicsDevice gs = ge.getDefaultScreenDevice();
					gc = gs.getDefaultConfiguration();
				}
				// a compatible image may be faster than a buffered image of fixed type as used for the headless case,
				// see https://www.java.net/node/693786
				result = gc.createCompatibleImage(width, height);
			}
			final Graphics2D g = result.createGraphics();
			try {
				render(g, rects);
			} finally {
				g.dispose();
			}
			return result;
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public BufferedImage build(final int width, final int height) {
		final ITreeModel<IRectangle<Node>> result;
		if (layout instanceof IGenericTreeMapLayout) {
			result = ((IGenericTreeMapLayout<Node, Number>) layout).layout((IGenericWeightedTreeModel<Node, Number>) model, currentRoot, width, height, () -> false);
		} else if (layout instanceof ITreeMapLayout) {
			result = layout.layout((IWeightedTreeModel<Node>) model, currentRoot, width, height, () -> false);
		} else {
			throw new IllegalStateException("cannot handle model with layout " + layout);
		}
		return rebuildImage(width, height, result);
	}

}
