package org.openlca.app.results.analysis.sankey.actions;

import org.eclipse.gef.ui.actions.SelectionAction;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.analysis.sankey.SankeyEditor;
import org.openlca.app.results.analysis.sankey.edit.SankeyNodeEditPart;

public class OpenEditorAction extends SelectionAction {

	/**
	 * The selected on which the action is called.
	 */
	private Object object;

	public OpenEditorAction(SankeyEditor part) {
		super(part);
		setId(ActionIds.OPEN_EDITOR);
		setImageDescriptor(Icon.FOLDER.descriptor());
	}

	@Override
	protected boolean calculateEnabled() {
		var objects = getSelectedObjects();
		if (objects.size() != 1)
			return false;
		object = objects.get(0);
		setText(M.OpenInEditor + ": " + getObjectName());

		return object instanceof SankeyNodeEditPart;
	}

	protected String getObjectName() {
		if (object == null)
			return "";

		if (object instanceof SankeyNodeEditPart)
			return M.Process;
		else return "";
	}

	@Override
	public void run() {
		if (object instanceof SankeyNodeEditPart nodeEditPart) {
			var node = nodeEditPart.getModel();
			App.open(node.product.provider());
		}
	}

}
