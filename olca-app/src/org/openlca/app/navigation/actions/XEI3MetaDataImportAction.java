package org.openlca.app.navigation.actions;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;
import org.openlca.io.ecospold2.input.IsicCategoryTreeSync;
import org.openlca.io.ecospold2.input.PersonUpdate;
import org.openlca.io.ecospold2.input.SourceUpdate;

/**
 * Navigation extension that imports ecoinvent 3 meta data into an existing
 * database.
 */
class XEI3MetaDataImportAction extends Action implements
		INavigationAction {

	public XEI3MetaDataImportAction() {
		setImageDescriptor(Icon.EXTENSION.descriptor());
		setText("Import ecoinvent 3 meta data");
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
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
		DirectoryDialog dialog = new DirectoryDialog(UI.shell());
		dialog.setText("Master data directory");
		dialog.setMessage("Select the EcoSpold 02 directory that contains the "
				+ "master data.");
		String path = dialog.open();
		if (path == null)
			return;
		File masterDataDir = new File(path);
		if (!masterDataDir.isDirectory())
			return;
		File personFile = new File(masterDataDir, "Persons.xml");
		if (personFile.exists())
			new PersonUpdate(Database.get(), personFile).run();
		File sourceFile = new File(masterDataDir, "Sources.xml");
		if (sourceFile.exists())
			new SourceUpdate(Database.get(), sourceFile).run();
		updateIsicTree();
	}

	private void updateIsicTree() {
		App.run("Update flow categories",
				new IsicCategoryTreeSync(Database.get(), ModelType.FLOW));
		App.run("Update process categories",
				new IsicCategoryTreeSync(Database.get(), ModelType.PROCESS),
				Navigator::refresh);
	}
}
