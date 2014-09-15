package org.openlca.app.navigation.actions;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.ImageType;
import org.openlca.io.refdata.RefDataImport;

public class XRefDataImport extends Action implements INavigationAction {

	public XRefDataImport() {
		setImageDescriptor(ImageType.EXTENSION_ICON.getDescriptor());
		setText("Import reference data");
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
		File dir = FileChooser.forImport(FileChooser.DIRECTORY_DIALOG);
		if (dir == null)
			return;
		Cache.evictAll();
		RefDataImport refImport = new RefDataImport(dir, Database.get());
		App.run("Import reference data", refImport, new Runnable() {
			@Override
			public void run() {
				Cache.evictAll();
				Navigator.refresh();
			}
		});
	}

}
