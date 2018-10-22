package org.openlca.app.rcp;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.db.LinkingPropertiesPage;
import org.openlca.app.navigation.actions.ValidateAction;
import org.openlca.app.navigation.actions.db.DbCloseAction;
import org.openlca.app.navigation.actions.db.DbCompressAction;
import org.openlca.app.navigation.actions.db.DbCopyAction;
import org.openlca.app.navigation.actions.db.DbCreateAction;
import org.openlca.app.navigation.actions.db.DbDeleteAction;
import org.openlca.app.navigation.actions.db.DbExportAction;
import org.openlca.app.navigation.actions.db.DbImportAction;
import org.openlca.app.navigation.actions.db.DbPropertiesAction;
import org.openlca.app.navigation.actions.db.DbRenameAction;
import org.openlca.app.navigation.actions.db.DbUpdateManagerAction;
import org.openlca.app.util.Actions;

class DatabaseMenu implements IMenuListener {

	private DatabaseMenu(IMenuManager manager) {
		MenuManager menu = new MenuManager(M.Database, "Database.Menu");
		menu.setRemoveAllWhenShown(true);
		menu.addMenuListener(this);
		manager.add(menu);
	}

	static void addTo(IMenuManager manager) {
		new DatabaseMenu(manager);
	}

	@Override
	public void menuAboutToShow(IMenuManager menu) {
		menu.add(new DbCreateAction());
		menu.add(new DbImportAction());
		if (Database.getActiveConfiguration() == null)
			return;
		Action checkLinksAction = Actions.create(
				M.CheckLinkingProperties, null, () -> {
					LinkingPropertiesPage.show();
				});
		IAction[] actions = new IAction[] {
				new DbExportAction(),
				ValidateAction.forDatabase(),
				new DbCopyAction(),
				new DbRenameAction(),
				new DbDeleteAction(),
				new DbCloseAction(),
				checkLinksAction,
				new DbPropertiesAction(),
				new DbCompressAction(),
				new DbUpdateManagerAction(),
		};
		for (IAction a : actions) {
			menu.add(a);
		}
	}
}
