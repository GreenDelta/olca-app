package org.openlca.app.navigation.actions;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.io.refdata.RefDataExport;

class XRefDataExport extends Action implements INavigationAction {

	public XRefDataExport() {
		setImageDescriptor(Icon.EXTENSION.descriptor());
		setText("Export reference data");
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
		File dir = FileChooser.selectFolder();
		if (dir == null)
			return;
		RefDataExport export = new RefDataExport(dir, Database.get());
		App.run("Export reference data", export);
	}

}
