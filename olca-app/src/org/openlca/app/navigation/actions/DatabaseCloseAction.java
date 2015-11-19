package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Editors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Close the activated database */
public class DatabaseCloseAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(getClass());

	public DatabaseCloseAction() {
		setText(Messages.CloseDatabase);
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
		Editors.closeAll();
		App.run(Messages.CloseDatabase, new Runnable() {
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
