package org.openlca.app.navigation.actions.libraries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.wizards.io.ImportLibraryDialog;
import org.openlca.core.database.IDatabase;
import org.openlca.core.library.Library;

public class AddLibraryAction extends Action implements INavigationAction {

	public AddLibraryAction() {
		setText(M.AddLibrary);
		setImageDescriptor(Icon.LIBRARY.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		return selection.get(0) instanceof DatabaseElement e
				&& Database.isActive(e.getContent());
	}

	static Library askForLibrary() {
		var db = Database.get();
		if (db == null) {
			MsgBox.error(M.NoDatabaseOpened);
			return null;
		}
		var dialog = new Dialog(db);
		if (dialog.open() != Window.OK
				|| dialog.combo == null
				|| dialog.combo.selected == null)
			return null;

		var lib = dialog.combo.selected;
		if (db.getLibraries().contains(lib.name())) {
			MsgBox.error(M.TheLibraryIsAlreadyPresent + " - " + lib.name());
			return null;
		}
		return lib;
	}

	@Override
	public void run() {
		var lib = askForLibrary();
		if (lib == null)
			return;
		LibraryActions.mount(lib);
	}

	private static class Dialog extends FormDialog {

		private final IDatabase db;
		private LibCombo combo;

		Dialog(IDatabase db) {
			super(UI.shell());
			this.db = db;
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(M.AddLibrary + " - " + db.getName());
		}

		@Override
		protected Point getInitialSize() {
			return new Point(500, 350);
		}

		@Override
		protected void createFormContent(IManagedForm form) {
			var tk = form.getToolkit();
			var body = UI.dialogBody(form.getForm(), tk);
			UI.gridLayout(body, 1);

			var comp = tk.createComposite(body);
			UI.fillHorizontal(comp);
			UI.gridLayout(comp, 2);

			combo = new LibCombo(UI.labeledCombo(comp, tk, M.Library));
			UI.filler(comp, tk);
			var importButton = tk.createButton(
					comp, M.ImportFromFileDots, SWT.NONE);
			Controls.onSelect(importButton, $ -> {
				var file = FileChooser.openFile()
						.withTitle(M.SelectLibraryPackage)
						.withExtensions("*.zip")
						.select()
						.orElse(null);
				if (file == null)
					return;
				ImportLibraryDialog.open(file)
						.ifPresent(combo::updateWith);
			});
		}
	}

	static class LibCombo {

		private final List<Library> libraries = new ArrayList<>();
		private final Combo combo;
		private Library selected;

		LibCombo(Combo combo) {
			this.combo = combo;
			fill();
			if (!libraries.isEmpty()) {
				select(libraries.get(0));
			}
			Controls.onSelect(combo, $ -> {
				var idx = combo.getSelectionIndex();
				if (idx >= 0) {
					selected = libraries.get(idx);
				}
			});
		}

		void updateWith(Library newLib) {
			fill();
			select(newLib);
		}

		private void fill() {
			var db = Database.get();
			var dbLibs = db != null
					? db.getLibraries()
					: Collections.emptySet();
			libraries.clear();
			Workspace.getLibraryDir()
					.getLibraries()
					.stream()
					.filter(lib -> !dbLibs.contains(lib.name()))
					.forEach(libraries::add);
			libraries.sort(Comparator.comparing(Library::name));
			var items = libraries.stream()
					.map(Library::name)
					.toArray(String[]::new);
			combo.setItems(items);
		}

		private void select(Library lib) {
			if (lib == null) {
				selected = null;
				return;
			}
			int idx = libraries.indexOf(lib);
			if (idx < 0)
				return;
			combo.select(idx);
			selected = lib;
		}

	}
}
