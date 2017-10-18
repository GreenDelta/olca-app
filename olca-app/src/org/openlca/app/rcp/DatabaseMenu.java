package org.openlca.app.rcp;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.actions.db.DbCloseAction;
import org.openlca.app.navigation.actions.db.DbCopyAction;
import org.openlca.app.navigation.actions.db.DbCreateAction;
import org.openlca.app.navigation.actions.db.DbDeleteAction;
import org.openlca.app.navigation.actions.db.DbExportAction;
import org.openlca.app.navigation.actions.db.DbImportAction;
import org.openlca.app.navigation.actions.db.DbPropertiesAction;
import org.openlca.app.navigation.actions.db.DbRenameAction;
import org.openlca.app.navigation.actions.db.DbValidateAction;
import org.openlca.app.navigation.actions.db.DbUpdateManagerAction;

class DatabaseMenu implements IMenuListener {

	private final DbCreateAction createAction;
	private final DbImportAction importAction;
	private final IAction[] actions;

	private DatabaseMenu(IMenuManager manager) {
		MenuManager menu = new MenuManager(M.Database, "Database.Menu");
		menu.setRemoveAllWhenShown(true);
		menu.addMenuListener(this);
		manager.add(menu);
		createAction = new DbCreateAction();
		importAction = new DbImportAction();
		actions = new IAction[] {
				new DbExportAction(),
				new DbValidateAction(),
				new DbCopyAction(),
				new DbCloseAction(),
				new DbPropertiesAction(),
				new DbUpdateManagerAction(),
				new DbRenameAction(),
				new DbDeleteAction()
		};
	}

	static void addTo(IMenuManager manager) {
		new DatabaseMenu(manager);
	}

	@Override
	public void menuAboutToShow(IMenuManager menu) {
		menu.add(createAction);
		menu.add(importAction);
		if (Database.get() != null) {
			for (IAction a : actions) {
				menu.add(a);
			}
		}
	}
}
