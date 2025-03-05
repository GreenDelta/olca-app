package org.openlca.app.navigation.actions.libraries;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.DataPackageElement;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Popup;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryPackage;

public class ExportLibraryAction extends Action implements INavigationAction {

	private Library library;

	public ExportLibraryAction() {
		setText(M.Export);
		setImageDescriptor(Icon.EXPORT.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DataPackageElement elem) || !elem.getContent().isLibrary())
			return false;
		library = Workspace.getLibraryDir().getLibrary(
				elem.getContent().name()).orElse(null);
		return library != null;
	}

	@Override
	public void run() {
		if (library == null)
			return;
		var target = FileChooser.forSavingFile(
				M.Export, library.name() + ".zip");
		if (target == null)
			return;
		try {
			App.runWithProgress(
					M.ExportLibraryDots,
					() -> LibraryPackage.zip(library, target),
					() -> Popup.info(M.LibraryExported + " - " + target.getName()));
		} catch (Exception e) {
			ErrorReporter.on("Failed to export library "
					+ library.name() + " to file "
					+ target.getName(), e);
		}
	}
}
