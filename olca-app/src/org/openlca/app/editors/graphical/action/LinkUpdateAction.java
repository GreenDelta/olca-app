package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.matrix.LinkingConfig;

public class LinkUpdateAction extends Action implements GraphAction {

	private GraphEditor editor;

	@Override
	public boolean accepts(GraphEditor editor) {
		this.editor = editor;
		return true;
	}

	@Override
	public void run() {
		if (editor == null)
			return;
		var dialog = new Dialog();
		if ( dialog.open() != Window.OK)
			return;
		// TODO: run the LinkUpdate
	}

	private class Dialog extends FormDialog {

		Dialog() {
			super(UI.shell());
			setBlockOnOpen(true);
		}

		@Override
		protected Point getInitialSize() {
			return UI.initialSizeOf(this, 600, 600);
		}

		@Override
		protected void configureShell(Shell newShell) {
			if (newShell != null) {
				newShell.setText("Update links");
			}
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var tk = mform.getToolkit();
			var body = UI.formBody(mform.getForm(), tk);
			UI.gridLayout(body, 2);

			var providerCombo = UI.formCombo(body, tk, "Update method");
			var providerOptions = LinkingConfig.DefaultProviders.values();
			var items = new String[providerOptions.length];
			for (int i = 0; i <items.length; i++) {
				var option = providerOptions[i];
				items[i] = Labels.of(option);
			}



		}
	}


}
