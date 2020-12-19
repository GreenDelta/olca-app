package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.viewers.ISelection;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ProcessNode;

class OpenAction extends EditorAction {

	private ProcessNode node;

	OpenAction() {
		setId(ActionIds.OPEN);
		setText(M.OpenInEditor);
	}

	@Override
	protected boolean accept(ISelection s) {
		node = GraphActions.firstSelectedOf(s, ProcessNode.class);
		return node != null;
	}

	@Override
	public void run() {
		App.open(node.process);
	}

}
