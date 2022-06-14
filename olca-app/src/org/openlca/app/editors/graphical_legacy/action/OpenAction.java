package org.openlca.app.editors.graphical_legacy.action;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.editors.graphical_legacy.GraphEditor;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;

public class OpenAction extends Action implements GraphAction {

	private ProcessNode node;

	public OpenAction() {
		setText(M.OpenInEditor);
	}

	@Override
	public boolean accepts(GraphEditor editor) {
		var list = GraphActions.allSelectedOf(editor, ProcessNode.class);
		if (list.size() != 1)
			return false;
		node = list.get(0);
		return true;
	}

	@Override
	public void run() {
		if (node != null && node.process != null) {
			App.open(node.process);
		}
	}

}
