package org.openlca.app.editors.graphical.edit;

import java.util.stream.Stream;

import org.eclipse.gef.editpolicies.SelectionEditPolicy;
import org.openlca.app.components.graphics.figures.SelectableConnection;

public class NodeSelectionPolicy extends SelectionEditPolicy {

	@Override
	public NodeEditPart getHost() {
		return (NodeEditPart) super.getHost();
	}

	@Override
	protected void hideSelection() {
		setSelected(false);
	}

	@Override
	protected void showPrimarySelection() {
		setSelected(true);
	}

	@Override
	protected void showSelection() {
		setSelected(true);
	}

	private void setSelected(boolean b) {
		var connections = Stream.concat(
						getHost().getModel().getAllSourceConnections().stream(),
						getHost().getModel().getAllTargetConnections().stream())
				.toList();
		for (var connection : connections) {
			var registry = getHost().getViewer().getEditPartRegistry();
			var part = registry.get(connection);
			if (part instanceof LinkEditPart link)
				if (link.getFigure() instanceof SelectableConnection con)
					con.setSelected(b);
		}
	}

}
