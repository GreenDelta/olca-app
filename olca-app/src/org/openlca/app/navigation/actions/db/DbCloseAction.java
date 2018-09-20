package org.openlca.app.navigation.actions.db;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.cloud.ui.commits.HistoryView;
import org.openlca.app.db.Database;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.editors.Editors;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.validation.ValidationView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Close the activated database */
public class DbCloseAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(getClass());

	public DbCloseAction() {
		setText(M.CloseDatabase);
		setImageDescriptor(Icon.DISCONNECT.descriptor());
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
		if (!Editors.closeAll())
			return;
		App.run(M.CloseDatabase, new Runnable() {
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
				HistoryView.refresh();
				ValidationView.clear();
			}
		});
	}

}
