package org.openlca.app.editors.graphical_legacy.action;

import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.editors.graphical_legacy.GraphConfig;
import org.openlca.app.editors.graphical_legacy.GraphEditor;
import org.openlca.app.editors.graphical_legacy.themes.Themes;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;

public class GraphSettingsAction extends Action {

	private final GraphEditor editor;

	public GraphSettingsAction(GraphEditor editor) {
		this.editor = editor;
		setId("GraphSettingsAction");
		setText(M.Settings);
		setImageDescriptor(Icon.PREFERENCES.descriptor());
	}

	@Override
	public void run() {
		if (editor == null)
			return;
		var config = GraphConfig.from(editor.config);
		if (new Dialog(config).open() != Window.OK)
			return;
		var route = config.isRouted != editor.config.isRouted;
		config.copyTo(editor.config);
		if (route) {
			editor.route();
		}
		editor.refresh();
		editor.setDirty();
		editor.getModel().figure.repaint();
	}

	private static class Dialog extends FormDialog {

		private final GraphConfig config;

		Dialog(GraphConfig config) {
			super(UI.shell());
			setBlockOnOpen(true);
			this.config = config;
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var tk = mform.getToolkit();
			var body = UI.formBody(mform.getForm(), tk);
			UI.gridLayout(body, 2);
			themeCombo(tk, body);

			// routed check
			UI.filler(body, tk);
			var routed = tk.createButton(body, "Routed connections", SWT.CHECK);
			routed.setSelection(config.isRouted);
			Controls.onSelect(routed,
					e -> config.isRouted = routed.getSelection());

			// show icons
			UI.filler(body, tk);
			var icons = tk.createButton(body, "Show flow icons", SWT.CHECK);
			icons.setSelection(config.showFlowIcons);
			Controls.onSelect(icons,
					e -> config.showFlowIcons = icons.getSelection());

			// show elementary flows
			UI.filler(body, tk);
			var elems = tk.createButton(body, "Show elementary flows", SWT.CHECK);
			elems.setSelection(config.showElementaryFlows);
			Controls.onSelect(elems,
					e -> config.showElementaryFlows = elems.getSelection());

			// show amounts
			UI.filler(body, tk);
			var amounts = tk.createButton(body, "Show flow amounts", SWT.CHECK);
			amounts.setSelection(config.showFlowAmounts);
			Controls.onSelect(amounts,
					e -> config.showFlowAmounts = amounts.getSelection());

			// edit mode
			UI.filler(body, tk);
			var isProcessEditingEnabled = tk.createButton(body, "Enable process editing", SWT.CHECK);
			isProcessEditingEnabled.setSelection(config.isProcessEditingEnabled);
			Controls.onSelect(isProcessEditingEnabled,
				e -> config.isProcessEditingEnabled =
					isProcessEditingEnabled.getSelection());
		}

		private void themeCombo(FormToolkit tk, Composite comp) {
			var combo = UI.formCombo(comp, tk, "Theme");
			UI.gridData(combo, true, false);
			var themes = Themes.loadFromWorkspace();
			var current = config.theme();
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
				config.theme(next);
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
