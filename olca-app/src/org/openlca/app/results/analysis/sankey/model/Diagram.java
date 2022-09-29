package org.openlca.app.results.analysis.sankey.model;

import org.openlca.app.results.analysis.sankey.SankeyConfig;
import org.openlca.app.results.analysis.sankey.SankeyEditor;
import org.openlca.app.tools.graphics.model.Component;
import org.openlca.core.results.Sankey;

public class Diagram extends Component {

	public final SankeyEditor editor;
	public final int orientation;
	private double zoom = 1.0;

	public Diagram(SankeyEditor editor, int orientation) {
		this.editor = editor;
		this.orientation = orientation;
	}

	public double getZoom() {
		return zoom;
	}

	public SankeyNode getNode(Sankey.Node node) {
		for (var child : getChildren())
			if (child instanceof SankeyNode sankeyNode)
				if (sankeyNode.node == node)
					return sankeyNode;
		return null;
	}

	public boolean isReferenceNode(SankeyNode node) {
		var refNode = getNode(editor.getSankey().root);
		return refNode.equals(node);
	}

	public SankeyConfig getConfig() {
		return editor.config;
	}

	public String toString() {
		return "DiagramModel";
	}

}
