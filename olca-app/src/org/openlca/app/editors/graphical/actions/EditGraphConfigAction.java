package org.openlca.app.editors.graphical.actions;

import java.util.HashMap;
import java.util.Objects;

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
import org.openlca.app.editors.graphical.themes.Themes;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Popup;
import org.openlca.app.util.UI;

import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.*;

public class EditGraphConfigAction extends WorkbenchPartAction {

	public static final String KEY_CONFIG = "config";

	private final GraphEditor editor;
	private GraphConfig config;

	public EditGraphConfigAction(GraphEditor part) {
		super(part);
		this.editor = part;
		setId(ActionIds.EDIT_GRAPH_CONFIG);
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

			editor.setDirty();
		}
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
		protected void createFormContent(IManagedForm managedForm) {
			var tk = managedForm.getToolkit();
			var body = UI.formBody(managedForm.getForm(), tk);
			UI.gridLayout(body, 2);
			themeCombo(tk, body);

			// routed check
			UI.filler(body, tk);
			var routed = tk.createButton(body, "Routed connections", SWT.CHECK);
			routed.setSelection(config.isRouted());
			Controls.onSelect(routed,
					e -> config.setRouted(routed.getSelection()));

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

		private void themeCombo(FormToolkit tk, Composite comp) {
			var combo = UI.formCombo(comp, tk, "Theme");
			UI.gridData(combo, true, false);
			var themes = Themes.loadFromWorkspace();
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
			int width = shell.x > 0 && shell.x < 600
					? shell.x
					: 600;
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
