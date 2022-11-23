package org.openlca.app.editors.graphical.actions;

import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphConfig;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.rcp.images.Icon;

public class EditModeAction extends GraphConfigAction {

	public EditModeAction(GraphEditor part) {
		super(part);
	}

	@Override
	protected void init() {
		setId(GraphActionIds.EDIT_MODE);
		setText(M.EditMode);
		setImageDescriptor(Icon.EDIT.descriptor());
		if (getEditor() != null)
			setChecked(getEditor().config.isNodeEditingEnabled());
	}

	@Override
	protected boolean editConfig() {
		var config = GraphConfig.from(getEditor().config);
		if (config == null)
			return false;
		else {
			config.setNodeEditingEnabled(!config.isNodeEditingEnabled());
			setNewConfig(config);
			return true;
		}
	}

}
