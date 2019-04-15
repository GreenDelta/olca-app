package org.openlca.app.tools.mapping;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.tools.mapping.model.JsonProvider;
import org.openlca.app.util.UI;

class JsonImportDialog extends Dialog {

	private final JsonProvider provider;

	static void open(JsonProvider provider) {
		if (provider == null)
			return;
		JsonImportDialog dialog = new JsonImportDialog(provider);
		dialog.open();
		// TODO: open mappings in editor...
	}

	JsonImportDialog(JsonProvider provider) {
		super(UI.shell());
		setBlockOnOpen(true);
		this.provider = provider;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Open flow mapping");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);
		UI.gridLayout(comp, 1);
		return comp;
	}
}
