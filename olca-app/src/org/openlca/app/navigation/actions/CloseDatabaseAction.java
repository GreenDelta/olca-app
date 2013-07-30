package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.resources.ImageType;
import org.openlca.core.application.App;
import org.openlca.core.application.db.Database;
import org.openlca.core.application.db.IDatabaseConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Close the activated database */
public class CloseDatabaseAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(getClass());

	public CloseDatabaseAction() {
		setText("Close");
		setImageDescriptor(ImageType.DISCONNECT_ICON.getDescriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement e = (DatabaseElement) element;
		IDatabaseConfiguration config = e.getContent();
		return Database.isActive(config);
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		App.run("Closing database", new Runnable() {
			public void run() {
				try {
					Database.close();
				} catch (Exception e) {
					log.error("Failed to close database", e);
				}
			}
		}, new Runnable() {
			public void run() {
				Navigator.refresh();
			}
		});
	}

}
