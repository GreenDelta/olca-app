package org.openlca.app.editors.graphical.actions;

import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphConfig;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.rcp.images.Images;
import org.openlca.core.model.FlowType;

public class ShowElementaryFlowsAction extends GraphConfigAction {

	public ShowElementaryFlowsAction(GraphEditor part) {
		super(part);
	}

	@Override
	protected void init() {
		setId(GraphActionIds.SHOW_ELEMENTARY_FLOWS);
		setText(M.ShowElementaryFlows);
		setImageDescriptor(Images.descriptor(FlowType.ELEMENTARY_FLOW));
		if (getEditor() != null)
			setChecked(getEditor().config.showElementaryFlows());
	}

	@Override
	protected boolean editConfig() {
		var config = GraphConfig.from(getEditor().config);
		if (config == null)
			return false;
		else {
			config.setShowElementaryFlows(!config.showElementaryFlows());
			setNewConfig(config);
			return true;
		}
	}

}
