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
			SaveScriptDialog.forImportOf(file);
			return;
		}

		var format = Format.detect(file).orElse(null);
	}
}
