package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.resources.ImageType;
import org.openlca.core.model.ModelType;
import org.openlca.io.ecospold2.IsicCategoryTreeSync;

/**
 * Navigation extension that imports ecoinvent 3 meta data into an existing
 * database.
 */
public class XEI3MetaDataImportAction extends Action implements
		INavigationAction {

	public XEI3MetaDataImportAction() {
		setImageDescriptor(ImageType.IMPORT_ICON.getDescriptor());
		setText("Import ecoinvent 3 meta data");
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
		App.run("Update flow categories",
				new IsicCategoryTreeSync(Database.get(), ModelType.FLOW));
		App.run("Update process categories",
				new IsicCategoryTreeSync(Database.get(), ModelType.PROCESS),
				new Runnable() {
					@Override
					public void run() {
						Navigator.refresh();
					}
				});
	}
}
