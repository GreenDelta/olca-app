package org.openlca.app.navigation.actions.db;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.Action;
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
	public boolean accept(INavigationElement<?> elem) {
		if (!(elem instanceof DatabaseElement))
			return false;
		var e = (DatabaseElement) elem;
		return Database.isActive(e.getContent());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
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
			var combo = UI.formCombo(body, tk, "Form workspace:");
			combo.setItems(items);
			if (items.length > 0) {
				combo.select(0);
				selectedInfo = infos[0];
			} else {
				var ok = getButton(OK);
				ok.setEnabled(false);
			}
			Controls.onSelect(combo, _e -> {
				int idx = combo.getSelectionIndex();
				if (idx < 0)
					return;
				selectedInfo = infos[idx];
			});
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
