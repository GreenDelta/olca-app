package org.openlca.app.navigation.actions.libraries;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryPackage;
import org.openlca.core.library.MountCheck;
import org.openlca.util.Strings;

public class AddLibraryAction extends Action implements INavigationAction {

	public AddLibraryAction() {
		setText("Add a library");
		setImageDescriptor(Icon.DATABASE.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		return selection.get(0) instanceof DatabaseElement e
			&& Database.isActive(e.getContent());
	}

	@Override
	public void run() {
		var db = Database.get();
		if (db == null) {
			MsgBox.error(M.NoDatabaseOpened);
			return;
		}
		new Dialog(db).open();
	}

	public static void run(File file) {
		if (file == null)
			return;
		new FileImportDialog(file).open();
	}

	private static Library extract(File file) {
		if (file == null)
			return null;

		// load the library meta data
		var info = LibraryPackage.getInfo(file);
		if (info == null) {
			MsgBox.error(file.getName()
				+ " is not a valid library package.");
			return null;
		}

		var id = Strings.notEmpty(info.version())
			? info.name() + " " + info.version()
			: info.name();

		// check if it already exists
		var libDir = Workspace.getLibraryDir();
		if (libDir.hasLibrary(id)) {
			MsgBox.error("A library " + id + " already exists.");
			return null;
		}

		// extract the library
		var extracted = App.exec("Extract library", () -> {
			LibraryPackage.unzip(file, libDir);
			return true;
		});
		if (extracted == null || !extracted)
			return null;
		Navigator.refresh();
		return libDir.getLibrary(id).orElse(null);
	}

	private static boolean mount(Library lib, IDatabase db) {
		if (lib == null || db == null)
			return false;
		if (!canMount(lib, db))
			return false;
		App.runWithProgress(
			"Mounting library " + lib.id() + " to " + db.getName(),
			() -> lib.mountTo(db),
			Navigator::refresh);
		return true;
	}

	private static boolean canMount(Library lib, IDatabase db) {
		if (lib == null) {
			MsgBox.error("Library does not exist in workspace.");
			return false;
		}
		if (db.getLibraries().contains(lib.id())) {
			MsgBox.error("Library " + lib.id() + " is already present.");
			return false;
		}
		var state = App.exec("Check library", () -> MountCheck.check(db, lib));
		if (state.isError()) {
			ErrorReporter.on("Failed to check library", state.error());
			return false;
		}
		return state.isUsed()
			? ForceMountQuestion.ask()
			: state.isOk();
	}

	private static class ForceMountQuestion extends FormDialog {

		private boolean forceMount = false;

		static boolean ask() {
			var q = new ForceMountQuestion();
			q.open();
			return q.forceMount;
		}

		ForceMountQuestion() {
			super(UI.shell());
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Data conflicts");
			newShell.setImage(Icon.ERROR.get());
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var tk = mForm.getToolkit();
			var body = UI.formBody(mForm.getForm(), tk);
			UI.gridLayout(body, 1);
			var text = tk.createFormText(body, false);
			text.setText("There are data sets that are contained" +
				" in the database and the library.", false, false);
			var check = tk.createButton(body, "Convert data sets in database to " +
				"library data sets (experimental)", SWT.CHECK);
			Controls.onSelect(check, $ -> forceMount = check.getSelection());
		}
	}

	private static class Dialog extends FormDialog {

		private final IDatabase db;
		private Library workspaceLib;
		private File externalFile;
		private boolean inWorkspaceMode = true;

		private Combo combo;
		private Text fileText;
		private Button fileBtn;

		Dialog(IDatabase db) {
			super(UI.shell());
			this.db = db;
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Add a library to " + db.getName());
		}

		@Override
		protected Point getInitialSize() {
			return new Point(500, 350);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var tk = mform.getToolkit();
			var body = UI.formBody(mform.getForm(), tk);
			UI.gridLayout(body, 1);

			// workspace section
			var workspaceCheck = tk.createButton(
				body, "From data folder:", SWT.RADIO);
			workspaceCheck.setSelection(inWorkspaceMode);
			createCombo(body);

			// file selector
			var fileCheck = tk.createButton(
				body, "From external file:", SWT.RADIO);
			fileCheck.setSelection(!inWorkspaceMode);
			createFileSection(tk, body);

			// switch between file and workspace modes
			Controls.onSelect(workspaceCheck, _e -> {
				inWorkspaceMode = workspaceCheck.getSelection();
				onSelectionChanged();
			});
			Controls.onSelect(fileCheck, _e -> {
				inWorkspaceMode = workspaceCheck.getSelection();
				onSelectionChanged();
			});
			onSelectionChanged(); // init
		}

		private void createCombo(Composite body) {
			// collect libraries that can mounted
			var dbLibs = db.getLibraries();
			var libs = Workspace.getLibraryDir()
				.getLibraries()
				.stream()
				.filter(lib -> !dbLibs.contains(lib.id()))
				.sorted((l1, l2) -> Strings.compare(l1.id(), l2.id()))
				.toArray(Library[]::new);
			var items = Arrays.stream(libs)
				.map(Library::id)
				.toArray(String[]::new);

			// create an fill the combo box
			combo = new Combo(body, SWT.READ_ONLY);
			UI.gridData(combo, true, false);
			combo.setItems(items);
			if (items.length > 0) {
				combo.select(0);
				workspaceLib = libs[0];
			}
			Controls.onSelect(combo, _e -> {
				int idx = combo.getSelectionIndex();
				if (idx < 0)
					return;
				workspaceLib = libs[idx];
				onSelectionChanged();
			});
		}

		private void createFileSection(FormToolkit tk, Composite body) {
			var fileComp = tk.createComposite(body);
			UI.gridLayout(fileComp, 2, 10, 0);
			UI.gridData(fileComp, true, false);
			fileText = tk.createText(fileComp,
				externalFile == null
					? ""
					: externalFile.getAbsolutePath());
			fileText.setEditable(false);
			UI.gridData(fileText, true, false);
			fileBtn = tk.createButton(fileComp, "Browse", SWT.NONE);
			Controls.onSelect(fileBtn, _e -> {
				var file = FileChooser.open("*.zip");
				if (file == null)
					return;
				externalFile = file;
				fileText.setText(file.getAbsolutePath());
				onSelectionChanged();
			});
		}

		private void onSelectionChanged() {
			combo.setEnabled(inWorkspaceMode);
			fileBtn.setEnabled(!inWorkspaceMode);
			fileText.setEnabled(!inWorkspaceMode);
			var ok = getButton(IDialogConstants.OK_ID);
			if (ok == null)
				return;
			ok.setEnabled(inWorkspaceMode
				? workspaceLib != null
				: externalFile != null);
		}

		@Override
		protected void okPressed() {
			var lib = inWorkspaceMode
				? workspaceLib
				: extract(externalFile);
			if (lib == null)
				return;
			if (mount(lib, db)) {
				super.okPressed();
			}
		}
	}

	private static class FileImportDialog extends FormDialog {

		private final File file;
		private boolean intoDB = false;

		FileImportDialog(File file) {
			super(UI.shell());
			this.file = file;
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Import library from file");
		}

		@Override
		protected Point getInitialSize() {
			return new Point(500, 350);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var tk = mform.getToolkit();
			var body = UI.formBody(mform.getForm(), tk);
			UI.gridLayout(body, 1);

			var workspaceCheck = tk.createButton(
				body, "Save in workspace only", SWT.RADIO);
			workspaceCheck.setSelection(true);

			var dbCheck = tk.createButton(
				body, "Add library to active database", SWT.RADIO);
			dbCheck.setSelection(false);
			var db = Database.get();
			dbCheck.setEnabled(db != null);

			Controls.onSelect(workspaceCheck,
				e -> intoDB = dbCheck.getSelection());
			Controls.onSelect(dbCheck,
				e -> intoDB = dbCheck.getSelection());
		}

		@Override
		protected void okPressed() {
			var info = extract(file);
			if (info == null)
				return;
			if (!intoDB) {
				super.okPressed();
				return;
			}
			if (mount(info, Database.get())) {
				super.okPressed();
			}
		}
	}
}
