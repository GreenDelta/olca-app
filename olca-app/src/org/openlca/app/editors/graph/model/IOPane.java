package org.openlca.app.editors.graph.model;

import org.openlca.app.editors.graph.GraphEditor;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.core.model.descriptors.RootDescriptor;

import java.util.List;

/**
 * An {@link IOPane} represents a list of exchanges to be displayed. Each
 * exchange is represented by a {@link ExchangeItem}.
 */
public class IOPane extends GraphComponent {

	private final boolean isInput;

	public IOPane(GraphEditor editor, boolean isInput) {
		super(editor);
		this.isInput = isInput;
	}

	public boolean getIsInput() {
		return isInput;
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

}
