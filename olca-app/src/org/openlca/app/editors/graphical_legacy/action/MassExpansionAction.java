package org.openlca.app.editors.graphical_legacy.action;

import org.eclipse.jface.viewers.ISelection;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Question;

class MassExpansionAction extends EditorAction {

	static final int EXPAND = 1;
	static final int COLLAPSE = 2;
	private final int type;

	MassExpansionAction(int type) {
		if (type == EXPAND) {
			setId(ActionIds.EXPAND_ALL);
			setText(M.ExpandAll);
			setImageDescriptor(Icon.EXPAND.descriptor());
		} else if (type == COLLAPSE) {
			setId(ActionIds.COLLAPSE_ALL);
			setText(M.CollapseAll);
			setImageDescriptor(Icon.COLLAPSE.descriptor());
		}
		this.type = type;
	}

	@Override
	public void run() {
		if (editor == null || editor.getModel() == null)
			return;

		// collapse all
		if (type == COLLAPSE) {
			editor.collapse();
			return;
		}

		// expand all; ask if the model is large
		var system = editor.getModel().getProductSystem();
		int count = system.processes.size();
		boolean doIt = count < 500 || Question.ask(
			M.ExpandAll, M.ExpandAll + ": " + count + " " + M.Processes);
		if (doIt) {
			editor.expand();
		}
	}

	@Override
	protected boolean accept(ISelection selection) {
		return true;
	}

}
