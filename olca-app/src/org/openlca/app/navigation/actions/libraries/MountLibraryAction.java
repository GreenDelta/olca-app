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
import org.openlca.app.preferences.FeatureFlag;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryImport;
import org.openlca.core.library.LibraryInfo;
import org.openlca.util.Strings;

public class MountLibraryAction extends Action implements INavigationAction {

	public MountLibraryAction() {
		setText("Add a library");
		setImageDescriptor(Icon.DATABASE.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (!FeatureFlag.LIBRARIES.isEnabled())
			return false;
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement))
			return false;
		var e = (DatabaseElement) first;
		return Database.isActive(e.getContent());
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

	private static class Dialog extends FormDialog {

		private final IDatabase db;
		private LibraryInfo workspaceLib;
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
			var infos = Workspace.getLibraryDir()
				.getLibraries()
				.stream()
				.map(Library::getInfo)
				.filter(info -> !dbLibs.contains(info.id()))
				.sorted((i1, i2) -> Strings.compare(
					i1.name + " " + i1.version,
					i2.name + " " + i2.version))
				.toArray(LibraryInfo[]::new);
			var items = Arrays.stream(infos)
				.map(info -> info.name + " " + info.version)
				.toArray(String[]::new);

			// create an fill the combo box
			combo = new Combo(body, SWT.READ_ONLY);
			UI.gridData(combo, true, false);
			combo.setItems(items);
			if (items.length > 0) {
				combo.select(0);
				workspaceLib = infos[0];
			}
			Controls.onSelect(combo, _e -> {
				int idx = combo.getSelectionIndex();
				if (idx < 0)
					return;
				workspaceLib = infos[idx];
				onSelectionChanged();
			});
		}

		private void createFileSection(FormToolkit tk, Composite body) {
			var fileComp = tk.createComposite(body);
			UI.gridLayout(fileComp, 2, 10, 0);
			UI.gridData(fileComp, true, false);
			fileText = tk.createText(fileComp, "");
			fileText.setEditable(false);
			UI.gridData(fileText, true, false);
			fileBtn = tk.createButton(fileComp, "Browse", SWT.NONE);
			Controls.onSelect(fileBtn, _e -> {
				var file = FileChooser.open("*.zip");
				if (file == null )
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
			if (inWorkspaceMode && workspaceLib == null)
				return;
			var dir = Workspace.getLibraryDir()
				.getFolder(workspaceLib);
			if (!dir.exists())
				return;
			var imp = new LibraryImport(db, new Library(dir));
			super.okPressed();
			App.runWithProgress(
				"Mount library " + workspaceLib.name
					+ " " + workspaceLib.version
					+ " to " + db.getName(),
				imp,
				Navigator::refresh);
		}
	}
}
