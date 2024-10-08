package org.openlca.app.results.analysis.sankey.actions;

import static org.openlca.app.results.analysis.sankey.SankeyConfig.CONFIG_PROP;
import static org.openlca.app.results.analysis.sankey.requests.SankeyRequestConstants.REQ_EDIT_CONFIG;

import java.util.HashMap;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.jface.window.Window;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.analysis.sankey.SankeyConfig;
import org.openlca.app.results.analysis.sankey.SankeyEditor;
import org.openlca.app.components.graphics.actions.ActionIds;
import org.openlca.app.util.Popup;

public class EditSankeyConfigAction extends WorkbenchPartAction {

	private final SankeyEditor editor;
	private SankeyConfig config;

	public EditSankeyConfigAction(SankeyEditor part) {
		super(part);
		this.editor = part;
		setId(ActionIds.EDIT_CONFIG);
		setText(M.Settings);
		setImageDescriptor(Icon.PREFERENCES.descriptor());
	}

	@Override
	public void run() {
		if (editor == null)
			return;
		config = SankeyConfig.from(editor.config, editor);
		var d = new SankeySelectionDialog(config, editor.items);
		if (d.open() != Window.OK)
			return;

		if (!config.equals(editor.config)) {
			if (getCommand().canExecute()) getCommand().execute();
			else Popup.info(M.FailedToApplyTheNewSettings);
		}
	}

	private Command getCommand() {
		var graphEditPart = editor.getRootEditPart().getContents();
		var request = new Request(REQ_EDIT_CONFIG);
		var data = new HashMap<String, Object>();
		data.put(CONFIG_PROP, config);
		request.setExtendedData(data);
		return graphEditPart.getCommand(request);
	}

	@Override
	protected boolean calculateEnabled() {
		return true;
	}

}
