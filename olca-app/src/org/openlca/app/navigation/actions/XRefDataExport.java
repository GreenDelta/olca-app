package org.openlca.app.navigation.actions;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.rcp.ImageType;
import org.openlca.io.refdata.RefDataExport;

public class XRefDataExport extends Action implements INavigationAction {

	public XRefDataExport() {
		setImageDescriptor(ImageType.EXTENSION_ICON.getDescriptor());
		setText("Export reference data");
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement e = (DatabaseElement) element;
		return Database.isActive(e.getContent());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		File dir = FileChooser.forExport(FileChooser.DIRECTORY_DIALOG);
		if (dir == null)
			return;
		RefDataExport export = new RefDataExport(dir, Database.get());
		App.run("Export reference data", export);
	}

}
