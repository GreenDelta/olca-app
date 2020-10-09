package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphConfig;
import org.openlca.app.editors.graphical.GraphEditor;
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
		if (new Dialog(config).open() == Window.OK) {
			config.applyOn(editor.config);
			editor.getModel().refreshChildren();
		}
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
			UI.gridLayout(body, 1);

			var icons = tk.createButton(body, "Show flow icons", SWT.CHECK);
			icons.setSelection(config.showFlowIcons);
			Controls.onSelect(icons,
					e -> config.showFlowIcons = icons.getSelection());

			var elems = tk.createButton(body, "Show elementary flows", SWT.CHECK);
			elems.setSelection(config.showElementaryFlows);
			Controls.onSelect(elems,
					e -> config.showElementaryFlows = elems.getSelection());

			var amounts = tk.createButton(body, "Show flow amounts", SWT.CHECK);
			amounts.setSelection(config.showFlowAmounts);
			Controls.onSelect(amounts,
					e -> config.showFlowAmounts = amounts.getSelection());
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
