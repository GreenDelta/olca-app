package org.openlca.app.tools.mapping;

import java.io.File;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.M;
import org.openlca.app.tools.mapping.model.JsonProvider;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Error;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JsonImportDialog extends Dialog {

	private final File file;
	private final JsonProvider provider;

	static void open(File file) {
		if (file == null)
			return;
		try {
			JsonProvider p = new JsonProvider(file);
			JsonImportDialog dialog = new JsonImportDialog(file, p);
			dialog.open();
			// TODO: open mappings in editor... which should then
			// close the provider.
			p.close();
		} catch (Exception e) {
			Error.showBox("Failed to open file as JSON-LD package");
			Logger log = LoggerFactory.getLogger(JsonImportDialog.class);
			log.error("failed to open JSON-LD package " + file, e);
		}

	}

	JsonImportDialog(File file, JsonProvider provider) {
		super(UI.shell());
		setBlockOnOpen(true);
		this.provider = provider;
		this.file = file;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Open flow mapping");
		UI.center(UI.shell(), shell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite root = (Composite) super.createDialogArea(parent);
		UI.gridLayout(root, 1, 10, 10);

		Composite comp = new Composite(root, SWT.NONE);
		UI.gridLayout(comp, 2, 10, 0);
		UI.formLabel(comp, M.File);
		String fileText = Strings.cut(file.getParent(), 50)
				+ File.separator
				+ Strings.cut(file.getName(), 50);
		Label label = UI.formLabel(comp, fileText);
		label.setForeground(Colors.linkBlue());

		Button openCheck = new Button(root, SWT.RADIO);
		openCheck.setText("Open mapping definition");
		Combo combo = new Combo(root, SWT.READ_ONLY);
		UI.gridData(combo, true, false);
		Controls.onSelect(openCheck, e -> {
			combo.setEnabled(true);
		});

		Button genCheck = new Button(root, SWT.RADIO);
		genCheck.setText("Generate mapping based on flow attributes");
		Controls.onSelect(genCheck, e -> {
			combo.setEnabled(false);
		});

		return root;
	}
}
