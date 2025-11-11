package org.openlca.app.editors.graphical.actions;

import static org.openlca.app.components.graphics.figures.Connection.ROUTER_CURVE;
import static org.openlca.app.components.graphics.figures.Connection.ROUTER_MANHATTAN;
import static org.openlca.app.components.graphics.figures.Connection.ROUTER_NULL;
import static org.openlca.app.editors.graphical.GraphConfig.CONFIG_PROP;
import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_EDIT_CONFIG;

import java.util.HashMap;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.graphics.actions.ActionIds;
import org.openlca.app.editors.graphical.GraphConfig;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Popup;
import org.openlca.app.util.UI;

public class EditGraphConfigAction extends WorkbenchPartAction {

	private final GraphEditor editor;
	private GraphConfig config;

	public EditGraphConfigAction(GraphEditor part) {
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
		config = GraphConfig.from(editor.config);
		if (new Dialog(config).open() != Window.OK)
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

	private static class Dialog extends FormDialog {

		private final GraphConfig config;

		Dialog(GraphConfig config) {
			super(UI.shell());
			setBlockOnOpen(true);
			this.config = config;
		}

		@Override
		protected void createFormContent(IManagedForm form) {
			var tk = form.getToolkit();
			var body = UI.dialogBody(form.getForm(), tk);
			UI.gridLayout(body, 2);

			// routed check
			connectionRoutersCombo(tk, body);

			// show elementary flows
			UI.filler(body, tk);
			var elems = tk.createButton(
				body, M.ShowElementaryFlows, SWT.CHECK);
			elems.setSelection(config.showElementaryFlows());
			Controls.onSelect(elems,
					e -> config.setShowElementaryFlows(elems.getSelection()));

			// edit mode
			UI.filler(body, tk);
			var isNodeEditingEnabled = tk.createButton(
				body, M.EnableProcessEditing, SWT.CHECK);
			isNodeEditingEnabled.setSelection(config.isNodeEditingEnabled());
			Controls.onSelect(isNodeEditingEnabled,
				e -> config.setNodeEditingEnabled(
					isNodeEditingEnabled.getSelection()));
		}

		private void connectionRoutersCombo(FormToolkit tk, Composite comp) {
			var combo = UI.labeledCombo(comp, tk, M.Connections);
			UI.gridData(combo, true, false);
			var connectionRouters = new String[]{
				ROUTER_NULL,
				ROUTER_MANHATTAN,
				ROUTER_CURVE
			};
			for (var router : connectionRouters) {
				combo.add(router);
			}
			combo.select(
					ArrayUtils.indexOf(connectionRouters, config.connectionRouter()));
			Controls.onSelect(combo, e -> {
				var router = connectionRouters[combo.getSelectionIndex()];
				config.setConnectionRouter(router);
			});
		}

		@Override
		protected Point getInitialSize() {
			var shell = getShell().getDisplay().getBounds();
			int width = shell.x > 0 && shell.x < 700
					? shell.x
					: 700;
			int height = shell.y > 0 && shell.y < 300
					? shell.y
					: 300;
			return new Point(width, height);
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(M.Settings);
		}
	}

}
