package org.openlca.app.results.analysis.sankey.edit;

import org.eclipse.gef.editpolicies.SelectionEditPolicy;
import org.openlca.app.tools.graphics.figures.SelectableConnection;

import java.util.stream.Stream;

public class SankeyNodeSelectionEditPolicy extends SelectionEditPolicy {

	@Override
	public SankeyNodeEditPart getHost() {
		return (SankeyNodeEditPart) super.getHost();
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

	@SuppressWarnings("unchecked")
	private void setSelected(boolean b) {
		var sources = getHost().getSourceConnections();
		var targets = getHost().getTargetConnections();
		var connections = Stream.concat(sources.stream(), targets.stream())
				.toList();
		for (var connection : connections) {
			if (connection instanceof LinkEditPart link) {
				if (link.getFigure() instanceof SelectableConnection con) {
					con.setSelected(b);
				}
			}
		}
	}

}
