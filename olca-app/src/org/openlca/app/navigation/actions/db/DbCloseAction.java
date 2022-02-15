package org.openlca.app.navigation.actions.db;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.views.CompareView;
import org.openlca.app.collaboration.views.HistoryView;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.slf4j.LoggerFactory;

/**
 * Close the activated database
 */
public class DbCloseAction extends Action implements INavigationAction {

	public DbCloseAction() {
		setText(M.CloseDatabase);
		setImageDescriptor(Icon.DISCONNECT.descriptor());
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
		if (!Editors.closeAll())
			return;
		App.run(M.CloseDatabase, () -> {
			try {
				Database.close();
			} catch (Exception e) {
				var log = LoggerFactory.getLogger(getClass());
				log.error("Failed to close database", e);
			}
		}, () -> {
			Navigator.refresh();
			HistoryView.refresh();
			CompareView.clear();
		});
	}

}
