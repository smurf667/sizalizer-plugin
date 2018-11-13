package de.engehausen.maven.sizalizer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import de.engehausen.treemap.IColorProvider;
import de.engehausen.treemap.ILabelProvider;
import de.engehausen.treemap.IRectangle;
import de.engehausen.treemap.IRectangleRenderer;
import de.engehausen.treemap.ITreeModel;
import de.engehausen.treemap.impl.CushionRectangleRenderer;
import de.engehausen.treemap.impl.LabelRenderer;
import de.engehausen.treemap.impl.StaticTreeMap;
import de.engehausen.treemap.impl.TreeModel;

public class Plain {

	public static void main(String[] args) throws IOException {
		Node root = new Node();
		root.label = "root";
		Node a = new Node(root);
		a.label = "statement";
		a.weight = 17;
		Node b = new Node(root);
		b.label = "edge";
		b.weight = 24;
		root.addChild(a);
		root.addChild(b);
		final TreeModel treeModel = new TreeModel(root);
		final StaticTreeMap treeMap = new StaticTreeMap();
		treeMap.setLabelProvider((model, rectangle) -> ((IRectangle<Node>) rectangle).getNode().label );
		treeMap.setColorProvider((model, rectangle) -> Color.RED );
		final IRectangleRenderer<Node, Graphics2D, Color> delegate = new LabelRenderer<Node>(new Font("SansSerif", Font.BOLD, 16));
		treeMap.setRectangleRenderer(new CushionRectangleRenderer<Node>(128) {
			@Override
			public void render(final Graphics2D graphics,
					final ITreeModel<IRectangle<Node>> model,
					final IRectangle<Node> rectangle,
					final IColorProvider<Node, Color> colorProvider,
					final ILabelProvider<Node> labelProvider) {
				super.render(graphics, model, rectangle, colorProvider, labelProvider);
				delegate.render(graphics, model, rectangle, colorProvider, labelProvider);
			}
		});
		treeMap.setTreeModel(treeModel);
		final BufferedImage result = treeMap.build(640, 512);
		ImageIO.write(result, "png", new FileOutputStream("C:\\temp\\treemap.png"));
	}

}
