package org.openlca.app.results.analysis.sankey.model;

import org.openlca.app.results.analysis.sankey.SankeyConfig;
import org.openlca.app.results.analysis.sankey.SankeyEditor;
import org.openlca.app.tools.graphics.model.BaseComponent;
import org.openlca.app.tools.graphics.model.Component;
import org.openlca.app.util.Labels;
import org.openlca.core.results.Sankey;

public class Diagram extends BaseComponent {

	public final SankeyEditor editor;
	public final int orientation;
	private SankeyNode referenceNode;

	public Diagram(SankeyEditor editor, int orientation) {
		this.editor = editor;
		this.orientation = orientation;
	}

	public SankeyEditor getEditor() {
		return editor;
	}

	public SankeyNode getNode(Sankey.Node node) {
		for (var child : getChildren()) {
			if (child instanceof SankeyNode sankeyNode) {
				if (sankeyNode.node.equals(node))
					return sankeyNode;
			}
		}
		return null;
	}

	public SankeyNode getReferenceNode() {
		if (referenceNode == null) {
			referenceNode = getNode(editor.getSankey().root);
		}
		return referenceNode;
	}

	public boolean isReferenceNode(SankeyNode node) {
		return getReferenceNode().equals(node);
	}

	public SankeyConfig getConfig() {
		return editor.config;
	}

	@Override
	public int compareTo(Component other) {
		return 0;
	}

	@Override
	public Component getFocusComponent() {
		return (editor != null && editor.getSankey() != null)
				? getReferenceNode()
				: null;
	}

	public String toString() {
		return "DiagramModel";
	}

}
