package org.openlca.app.navigation.actions.db;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
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

public class DbAddLibraryAction extends Action implements INavigationAction {

	public DbAddLibraryAction() {
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
		private LibraryInfo selectedInfo;

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

			// create and fill the combo
			var workspaceCheck = tk.createButton(
					body, "Form workspace:", SWT.RADIO);
			workspaceCheck.setSelection(true);
			var combo = new Combo(body, SWT.READ_ONLY);
			UI.gridData(combo, true, false);
			combo.setItems(items);
			if (items.length > 0) {
				combo.select(0);
				selectedInfo = infos[0];
			}
			Controls.onSelect(combo, _e -> {
				int idx = combo.getSelectionIndex();
				if (idx < 0)
					return;
				selectedInfo = infos[idx];
			});
			
			// file selector; not yet implemented
			var fileCheck = tk.createButton(
					body, "Form zip file:", SWT.RADIO);
			fileCheck.setSelection(false);
			fileCheck.setEnabled(false);
			var fileComp = tk.createComposite(body);
			UI.gridLayout(fileComp, 2, 10, 0);
			UI.gridData(fileComp, true, false);
			
			var fileText = tk.createText(fileComp, "");
			fileText.setEnabled(false);
			UI.gridData(fileText, true, false);
			var fileBtn = tk.createButton(fileComp, "Browse", SWT.NONE);
			fileBtn.setEnabled(false);
		}

		@Override
		protected void okPressed() {
			if (selectedInfo == null)
				return;
			var dir = Workspace.getLibraryDir()
					.getFolder(selectedInfo);
			if (!dir.exists())
				return;
			var imp = new LibraryImport(db, new Library(dir));
			super.okPressed();
			App.runWithProgress(
					"Mount library " + selectedInfo.name
							+ " " + selectedInfo.version
							+ " to " + db.getName(),
					imp,
					Navigator::refresh);
		}
	}
}
