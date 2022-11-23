package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.openlca.app.editors.graphical.GraphConfig;
import org.openlca.app.editors.graphical.GraphEditor;

import java.util.HashMap;

import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_EDIT_CONFIG;

abstract class GraphConfigAction extends WorkbenchPartAction {

	public static final String KEY_CONFIG = "config";

	private final GraphEditor editor;
	private GraphConfig newConfig;

	public GraphConfigAction(GraphEditor part) {
		super(part);
		this.editor = part;
	}

	@Override
	public void run() {
		if (editor == null)
			return;

		if (editConfig())
			if (getCommand().canExecute()) getCommand().execute();
	}

	protected abstract boolean editConfig();

	private Command getCommand() {
		var graphEditPart = editor.getRootEditPart().getContents();
		var request = new Request(REQ_EDIT_CONFIG);
		var data = new HashMap<String, Object>();
		data.put(KEY_CONFIG, newConfig);
		request.setExtendedData(data);
		return graphEditPart.getCommand(request);
	}

	@Override
	protected boolean calculateEnabled() {
		return editor != null && editor.config != null;
	}

	protected GraphEditor getEditor() {
		return editor;
	}

	protected void setNewConfig(GraphConfig config) {
		this.newConfig = config;
	}

}
