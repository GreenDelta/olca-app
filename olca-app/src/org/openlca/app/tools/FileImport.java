package org.openlca.app.tools;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.db.Database;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.io.Format;

public class FileImport {

	public void run() {
		var path = new FileDialog(UI.shell(), SWT.OPEN).open();
		if (path == null)
			return;
		var file = new File(path);
		if (!file.exists() || file.isDirectory()) {
			MsgBox.error("Not a file", path + " is not an existing file.");
			return;
		}

		// handle script files
		var name = file.getName().toLowerCase();
		if (name.endsWith(".py")
				|| name.endsWith(".sql")) {
			new ScriptDialog(file).open();
			return;
		}

		var format = Format.detect(file).orElse(null);
	}

	private static class ScriptDialog extends FormDialog {

		private final File file;
		private boolean asGlobal = true;

		ScriptDialog(File file) {
			super(UI.shell());
			this.file = file;
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
					+ file.getName() + "` as script?") ;

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


}
