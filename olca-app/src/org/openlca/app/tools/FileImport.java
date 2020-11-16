package org.openlca.app.tools;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.db.Database;
import org.openlca.app.devtools.SaveScriptDialog;
import org.openlca.app.navigation.actions.db.DbRestoreAction;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.app.wizards.io.EcoSpold01ImportWizard;
import org.openlca.core.database.IDatabase;
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
			SaveScriptDialog.forImportOf(file);
			return;
		}

		var format = Format.detect(file).orElse(null);
		if (format == null) {
			MsgBox.info("Unknown format",
					"openLCA could not detect the format of the file '"
							+ file.getName() + "'. You can also try an "
							+ "import option in the generic import dialog "
							+ "under Import > Other");
			return;
		}

		switch (format) {
			case ES1_XML:
			case ES1_ZIP:
				EcoSpold01ImportWizard.of(file);
				break;
			case ZOLCA:
				importZOLCA(file);
				break;
		}

	}

	private void importZOLCA(File file) {
		var db = Database.get();
		if (db == null) {
			var b = Question.ask("Import database?",
					"Import file '" + file.getName() + "' as new database?");
			if (b) {
				DbRestoreAction.run(file);
			}
			return;
		}
		new ZolcaImportDialog(file, db).open();
	}

	private static class ZolcaImportDialog extends FormDialog {

		private final File zolca;
		private final IDatabase activeDB;
		private boolean intoActiveDB = false;

		ZolcaImportDialog(File zolca, IDatabase activeDB) {
			super(UI.shell());
			this.zolca = zolca;
			this.activeDB = activeDB;
		}

		@Override
		protected void configureShell(Shell shell) {
			super.configureShell(shell);
			shell.setText("Import database");
			shell.setImage(Icon.IMPORT.get());
		}

		@Override
		protected Point getInitialSize() {
			return new Point(600, 400);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var tk = mform.getToolkit();
			var body = UI.formBody(mform.getForm(), tk);
			UI.gridLayout(body, 1);
			tk.createLabel(body,
					"Import file " + zolca.getName());

			var opt1 = tk.createButton(body,
					"As standalone database", SWT.RADIO);
			opt1.setSelection(!intoActiveDB);
			Controls.onSelect(opt1,
					_e -> intoActiveDB = !opt1.getSelection());

			var opt2 = tk.createButton(body,
					"Into the active database " +
							activeDB.getName(),
					SWT.RADIO);
			opt2.setSelection(intoActiveDB);
			Controls.onSelect(opt2,
					_e -> intoActiveDB = opt2.getSelection());

			mform.reflow(true);
		}

		@Override
		protected void okPressed() {
			if (!intoActiveDB) {
				DbRestoreAction.run(zolca);
				super.okPressed();
				return;
			}
			try {

				// TODO: run zolca import

			} catch (Exception e) {
				ErrorReporter.on("Failed to import *.zolca " +
						"file into active database", e);
			}
		}
	}
}
