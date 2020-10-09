package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.rcp.images.Icon;
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
		new Dialog().open();
	}

	private static class Dialog extends FormDialog {

		Dialog() {
			super(UI.shell());
			setBlockOnOpen(true);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var tk = mform.getToolkit();
			var body = UI.formBody(mform.getForm(), tk);
			UI.gridLayout(body, 1);
			tk.createButton(body, "Show flow icons", SWT.CHECK);
			tk.createButton(body, "Show elementary flows", SWT.CHECK);
			tk.createButton(body, "Show flow amounts", SWT.CHECK);
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
