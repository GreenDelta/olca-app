package org.openlca.app.wizards.io;

import java.io.File;
import java.util.Optional;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryInfo;
import org.openlca.core.library.LibraryPackage;

public class ImportLibraryDialog extends FormDialog {

	private final LibraryInfo info;

	public static Optional<Library> open(File file) {
		var info = LibraryPackage.getInfo(file);
		if (info == null) {
			MsgBox.error("Not a library package",
				"The file " + file.getName() + " is not a library package.");
			return Optional.empty();
		}
		var libDir = Workspace.getLibraryDir();
		var existing = libDir.getLibrary(info.name());
		if (existing.isPresent()) {
			MsgBox.error("Library " + info.name() + " already exists");
			return Optional.empty();
		}
		var dialog = new ImportLibraryDialog(info);
		if (dialog.open() != Window.OK)
			return Optional.empty();

		App.exec(
			"Import library " + info.name(),
			() -> LibraryPackage.unzip(file, Workspace.getLibraryDir()));
		var imported = libDir.getLibrary(info.name());
		if (imported.isEmpty()){
			MsgBox.error("Failed to import library");
		}
		Navigator.refresh();
		return imported;
	}

	private ImportLibraryDialog(LibraryInfo info) {
		super(UI.shell());
		this.info = info;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Import library " + info.name());
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 400);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.formBody(mForm.getForm(), tk);
		UI.gridLayout(body, 1);

		var comp = tk.createComposite(body);
		UI.gridData(comp, true, false);
		UI.gridLayout(comp, 2);

		// name & description
		var name = UI.formText(comp, tk, "Library");
		name.setEditable(false);
		Controls.set(name, info.name());
		var desc = UI.formMultiText(comp, tk, "Description");
		desc.setEditable(false);
		Controls.set(desc, info.description());

		// dependencies
		if (info.dependencies().isEmpty())
			return;
		UI.formLabel(comp, tk, "Dependencies");
		var depText = new StringBuilder("<ul>");
		var libDir = Workspace.getLibraryDir();
		for (var dep : info.dependencies()) {
			depText.append("<li>")
				.append(dep);
			if (libDir.getLibrary(dep).isPresent()) {
				depText.append(" (is present)");
			}
			depText.append("</li>");
		}
		depText.append("</ul>");
		tk.createFormText(comp, false)
			.setText(depText.toString(), true, false);
	}
}
