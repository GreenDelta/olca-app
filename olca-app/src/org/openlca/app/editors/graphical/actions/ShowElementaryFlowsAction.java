package org.openlca.app.editors.graphical.actions;

import java.util.HashMap;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphConfig;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.rcp.images.Images;
import org.openlca.core.model.FlowType;

import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.*;

public class ShowElementaryFlowsAction extends WorkbenchPartAction {

	public static final String KEY_CONFIG = "config";

	private final GraphEditor editor;
	private GraphConfig config;

	public ShowElementaryFlowsAction(GraphEditor part) {
		super(part);
		this.editor = part;
		setId(GraphActionIds.SHOW_ELEMENTARY_FLOWS);
		setText(M.ShowElementaryFlows);
		setImageDescriptor(Images.descriptor(FlowType.ELEMENTARY_FLOW));
		setChecked(editor.config.showElementaryFlows());
	}

	@Override
	public void run() {
		if (editor == null)
			return;
		config = GraphConfig.from(editor.config);
		config.setShowElementaryFlows(!config.showElementaryFlows());

		if (getCommand().canExecute()) getCommand().execute();
	}

	private Command getCommand() {
		var graphEditPart = editor.getRootEditPart().getContents();
		var request = new Request(REQ_EDIT_CONFIG);
		var data = new HashMap<String, Object>();
		data.put(KEY_CONFIG, config);
		request.setExtendedData(data);
		return graphEditPart.getCommand(request);
	}

	@Override
	protected boolean calculateEnabled() {
		return editor != null;
	}

}
