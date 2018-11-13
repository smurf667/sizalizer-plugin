package de.engehausen.treemap.impl;

import java.util.Collections;
import java.util.Iterator;

import de.engehausen.maven.sizalizer.Node;
import de.engehausen.treemap.IWeightedTreeModel;

public class TreeModel implements IWeightedTreeModel<Node> {

	private final Node root;

	public TreeModel(final Node root) {
		this.root = root;
	}

	@Override
	public Node getRoot() {
		return root;
	}

	@Override
	public Node getParent(final Node node) {
		return node != null ? node.parent : null;
	}

	@Override
	public Iterator<Node> getChildren(final Node node) {
		return node != null && node.children != null ? node.children.iterator() : Collections.emptyIterator();
	}

	@Override
	public boolean hasChildren(final Node node) {
		return getChildren(node).hasNext();
	}

	@Override
	public long getWeight(final Node node) {
		return node != null ? node.weight : 0;
	}

}
