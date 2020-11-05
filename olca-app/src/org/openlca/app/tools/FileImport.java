package org.openlca.app.tools;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.openlca.app.db.Database;
import org.openlca.app.devtools.SaveScriptDialog;
import org.openlca.app.navigation.actions.db.DbRestoreAction;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
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
		// TODO: show a dialog where the user can select between
		// a database restore or an import into the active database.
	}
}
