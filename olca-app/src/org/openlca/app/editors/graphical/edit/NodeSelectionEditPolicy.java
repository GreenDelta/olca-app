package org.openlca.app.editors.graphical.edit;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.editpolicies.SelectionEditPolicy;
import org.openlca.app.tools.graphics.figures.SelectableConnection;

import java.util.stream.Stream;

public class NodeSelectionEditPolicy extends SelectionEditPolicy {

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
			var linkEditPart = (EditPart) registry.get(connection);
			if (linkEditPart instanceof LinkEditPart link)
				if (link.getFigure() instanceof SelectableConnection con)
					con.setSelected(b);
		}
	}

}
