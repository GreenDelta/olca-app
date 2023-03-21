package org.openlca.app.editors.graphical.actions;

import java.util.HashMap;
import java.util.Objects;

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
import org.openlca.app.editors.graphical.GraphConfig;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.tools.graphics.actions.ActionIds;
import org.openlca.app.tools.graphics.themes.Themes;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Popup;
import org.openlca.app.util.UI;

import static org.openlca.app.editors.graphical.GraphConfig.CONFIG_PROP;
import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.*;
import static org.openlca.app.tools.graphics.figures.Connection.*;

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
			else Popup.info("Failed to apply the new settings");
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

			// Theme
			themeCombo(tk, body);

			// routed check
			connectionRoutersCombo(tk, body);

			// show elementary flows
			UI.filler(body, tk);
			var elems = tk.createButton(
				body, "Show elementary flows", SWT.CHECK);
			elems.setSelection(config.showElementaryFlows());
			Controls.onSelect(elems,
					e -> config.setShowElementaryFlows(elems.getSelection()));

			// edit mode
			UI.filler(body, tk);
			var isNodeEditingEnabled = tk.createButton(
				body, "Enable process editing", SWT.CHECK);
			isNodeEditingEnabled.setSelection(config.isNodeEditingEnabled());
			Controls.onSelect(isNodeEditingEnabled,
				e -> config.setNodeEditingEnabled(
					isNodeEditingEnabled.getSelection()));
		}

		private void connectionRoutersCombo(FormToolkit tk, Composite comp) {
			var combo = UI.labeledCombo(comp, tk, "Connections");
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

		private void themeCombo(FormToolkit tk, Composite comp) {
			var combo = UI.labeledCombo(comp, tk, "Theme");
			UI.gridData(combo, true, false);
			var themes = Themes.loadFromWorkspace(Themes.MODEL);
			var current = config.getTheme();
			int currentIdx = 0;
			for (int i = 0; i < themes.size(); i++) {
				var theme = themes.get(i);
				combo.add(theme.name());
				if (Objects.equals(theme.file(), current.file())) {
					currentIdx = i;
				}
			}
			combo.select(currentIdx);
			Controls.onSelect(combo, _e -> {
				var next = themes.get(combo.getSelectionIndex());
				config.setTheme(next);
			});
		}

		@Override
		protected Point getInitialSize() {
			var shell = getShell().getDisplay().getBounds();
			int width = shell.x > 0 && shell.x < 700
					? shell.x
					: 700;
			int height = shell.y > 0 && shell.y < 350
					? shell.y
					: 350;
			return new Point(width, height);
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(M.Settings);
		}
	}

}
