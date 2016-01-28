package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Question;

class MassExpansionAction extends Action {

	static final int EXPAND = 1;
	static final int COLLAPSE = 2;

	private int type;
	private ProductSystemGraphEditor editor;

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
		if (type == EXPAND) {
			if (areYouSure())
				editor.expand();
		} else if (type == COLLAPSE)
			editor.collapse();
	}

	private boolean areYouSure() {
		int amount = editor.getModel().getProductSystem().getProcesses().size();
		if (amount < 500)
			return true;
		String title = M.ExpandAll;
		String text = M.ExpandAll + ": " + amount + " "
				+ M.Processes;
		return Question.ask(title, text);
	}

	void setEditor(ProductSystemGraphEditor editor) {
		this.editor = editor;
	}

}
