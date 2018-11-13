package de.engehausen.maven.sizalizer;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class Node {

	public String label;
	public long weight;
	@JsonInclude(Include.NON_NULL)
	public List<Node> children;
	@JsonIgnore
	public final Node parent;

	public Node() {
		this(null);
	}
	
	public Node(final Node parent) {
		this.parent = parent;
	}
	
	public void addChild(final Node node) {
		if (node.parent != this) {
			throw new IllegalStateException("wrong parent");
		}
		if (children == null) {
			children = new ArrayList<>();
		}
		children.add(node);
		if (node.weight > 0) {
			Node current = this;
			while (current != null) {
				current.weight += node.weight;
				current = current.parent;
			}
		}
	}
	
	public String toString() {
		return label + ":" + weight + " (" + children + ")";
	}

}
