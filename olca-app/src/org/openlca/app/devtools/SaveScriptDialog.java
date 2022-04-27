package org.openlca.app.devtools;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;

import com.ibm.icu.text.CharsetDetector;

public class SaveScriptDialog extends FormDialog {

	private final String script;
	private String name;
	private boolean asGlobal = true;

	/**
	 * Holds the file that was written to the local workspace if that was
	 * successful.
	 */
	private File file;

	public static Optional<File> forImportOf(File file) {
		if (file == null || !file.exists())
			return Optional.empty();
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
			return forScriptOf(file.getName(), script);
		} catch (Exception e) {
			ErrorReporter.on("Failed to read file " + file, e);
			return Optional.empty();
		}
	}

	public static Optional<File> forScriptOf(String name, String script) {
		var dialog = new SaveScriptDialog(name, script);
		return dialog.open() == OK
				? Optional.ofNullable(dialog.file)
				: Optional.empty();
	}

	private SaveScriptDialog(String name, String script) {
		super(UI.shell());
		this.name = name == null
				? "script.py"
				: name;
		this.script = script;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Save script");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 350);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var tk = mform.getToolkit();
		var body = UI.formBody(mform.getForm(), tk);
		UI.gridLayout(body, 2);
		var text = UI.formText(body, tk, "File name:");
		text.setText(name);
		text.addModifyListener(e -> name = text.getText());

		// add a `save as local` script option when
		// a database is open
		var db = Database.get();
		if (db == null)
			return;

		UI.filler(body, tk);
		var global = tk.createButton(
				body, "As global script", SWT.RADIO);
		global.setSelection(true);
		Controls.onSelect(
				global, e -> asGlobal = global.getSelection());

		UI.filler(body, tk);
		var local = tk.createButton(
				body, "As script in database " + db.getName(),
				SWT.RADIO);
		local.setSelection(false);
		Controls.onSelect(
				local, e -> this.asGlobal = !local.getSelection());
	}

	@Override
	protected void okPressed() {
		if (name.isEmpty()) {
			MsgBox.error("Empty name",
					"An empty name is not allowed");
			return;
		}

		try {
			var file = new File(dir(), name);
			if (file.exists()) {
				MsgBox.error("Script already exists",
						"The script " + name + " already exists");
				return;
			}
			Files.writeString(file.toPath(),
					script, StandardCharsets.UTF_8);
			Navigator.refresh();
			super.okPressed();
			this.file = file;
		} catch (Exception e) {
			ErrorReporter.on("Failed to save script " + name, e);
		}
	}

	private File dir() throws Exception {
		var db = Database.get();
		if (asGlobal || db == null) {
			var dir = new File(Workspace.root(), "Scripts");
			Files.createDirectories(dir.toPath());
			return dir;
		}

		// jump to the global folder when there is
		// no file storage location for the database
		var dbDir = db.getFileStorageLocation();
		if (dbDir == null) {
			asGlobal = true;
			return dir();
		}
		var dir = new File(dbDir, "Scripts");
		Files.createDirectories(dir.toPath());
		return dir;
	}
}