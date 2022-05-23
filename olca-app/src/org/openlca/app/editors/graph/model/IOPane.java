package org.openlca.app.editors.graph.model;

import java.util.List;

import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graph.GraphEditor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;


/**
 * An {@link IOPane} represents a list of exchanges to be displayed. Each
 * exchange is represented by a {@link ExchangeItem}.
 */
public class IOPane extends GraphComponent {

	private final boolean forInputs;

	public IOPane(GraphEditor editor, boolean forInputs) {
		super(editor);
		this.forInputs = forInputs;
	}

	public boolean isForInputs() {
		return forInputs;
	}

	@SuppressWarnings("unchecked")
	public List<ExchangeItem> getExchangesItems() {
		return (List<ExchangeItem>) super.getChildren();
	}

	public Node getNode() {
		return (Node) getParent();
	}

	public Graph getGraph() {
		return getNode().getGraph();
	}

	public String toString() {
		var label = forInputs ? "input" : "output";
		return "IOPane(" + label + ")";
	}

}
