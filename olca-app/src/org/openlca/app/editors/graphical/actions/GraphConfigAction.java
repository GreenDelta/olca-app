package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.openlca.app.editors.graphical.GraphConfig;
import org.openlca.app.editors.graphical.GraphEditor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

import static org.openlca.app.editors.graphical.GraphConfig.CONFIG_PROP;
import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_EDIT_CONFIG;

abstract class GraphConfigAction extends WorkbenchPartAction implements
		PropertyChangeListener {

	private final GraphEditor editor;
	private GraphConfig newConfig;

	public GraphConfigAction(GraphEditor part) {
		super(part);
		this.editor = part;
		editor.config.addPropertyChangeListener(this);
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
		data.put(CONFIG_PROP, newConfig);
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

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (CONFIG_PROP.equals(prop))
			refreshCheck();

	}

	protected abstract void refreshCheck();

	@Override
	public void dispose() {
		getEditor().config.removePropertyChangeListener(this);
	}

}
