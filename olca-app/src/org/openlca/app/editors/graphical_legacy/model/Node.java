package org.openlca.app.editors.graphical_legacy.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.openlca.app.editors.graphical_legacy.GraphConfig;
import org.openlca.app.editors.graphical_legacy.GraphEditor;

/**
 * An abstract class for the model elements of the model graph.
 */
abstract class Node {

	public final GraphEditor editor;

	private Node parent;
	private final List<Node> children = new ArrayList<>();

	/**
	 * @deprecated a Node represents a model and should not contain a reference
	 * to a Figure; better move the figure to the corresponding EditPart
	 */
	@Deprecated
	public IFigure figure;

	AppAbstractEditPart<?> editPart;

	Node(GraphEditor editor) {
		this.editor = editor;
	}

	public GraphConfig config() {
		return editor != null
				? editor.config
				: new GraphConfig();
	}

	public Node parent() {
		return parent;
	}

	public boolean add(Node child) {
		if (child == null)
			return false;
		children.add(child);
		child.parent = this;
		if (editPart != null) {
			editPart.refreshChildren();
		}
		return true;
	}

	public boolean remove(Node child) {
		if (child == null)
			return false;
		if (!children.remove(child))
			return false;
		if (editPart != null)
			editPart.refreshChildren();
		return true;
	}

	public List<? extends Node> getChildren() {
		return children;
	}

	public boolean isVisible() {
		return figure != null && figure.isVisible();
	}

	public void setVisible(boolean value) {
		if (figure == null)
			return;
		figure.setVisible(value);
	}

	public Dimension getSize() {
		return figure != null ? figure.getSize() : new Dimension();
	}

}
