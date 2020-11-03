package org.openlca.app.devtools;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import com.ibm.icu.text.CharsetDetector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.db.Database;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;

public class SaveScriptDialog extends FormDialog {

	private String name;
	private String script;
	private boolean asGlobal = true;

	public static void forImportOf(File file) {
		if (file == null || !file.exists())
			return;
		// for imported scripts we try to detect the
		// encoding. in the openLCA workspace we save
		// everything encoded in utf-8
		try {
			var bytes = Files.readAllBytes(file.toPath());
			var match = new CharsetDetector().setText(bytes).detect();
			var charset = match == null || match.getName() == null
					? Charset.defaultCharset()
					: Charset.forName(match.getName());
			var script = new String(bytes, charset);
			forScriptOf(file.getName(), script);
		} catch (Exception e) {
			MsgBox.error("Failed to read file",
					"Failed to read file " + file
							+ ": " + e.getMessage());
		}
	}

	public static void forScriptOf(String name, String script) {

	}


	private SaveScriptDialog() {
		super(UI.shell());
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Import script");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 350);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var tk = mform.getToolkit();
		var body = UI.formBody(mform.getForm(), tk);
		UI.gridLayout(body, 1);
		tk.createLabel(body, "Import file `"
				+ file.getName() + "` as script?");

		// add a `save as local` script option when
		// a database is open
		var db = Database.get();
		if (Database.get() != null) {
			var global = tk.createButton(
					body, "As global script", SWT.RADIO);
			global.setSelection(true);
			Controls.onSelect(
					global, e -> asGlobal = global.getSelection());
			var local = tk.createButton(
					body, "As script in database " + db.getName(),
					SWT.RADIO);
			local.setSelection(false);
			Controls.onSelect(
					local, e -> this.asGlobal = !local.getSelection());
		}
	}

	@Override
	protected void okPressed() {


		super.okPressed();
	}
}