package org.openlca.app.rcp;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.db.LinkingPropertiesPage;
import org.openlca.app.db.tables.CurrencyTable;
import org.openlca.app.db.tables.FlowPropertyTable;
import org.openlca.app.db.tables.FlowTable;
import org.openlca.app.db.tables.ProcessTable;
import org.openlca.app.db.tables.UnitTable;
import org.openlca.app.editors.parameters.bigtable.BigParameterTable;
import org.openlca.app.navigation.actions.db.DbCloseAction;
import org.openlca.app.navigation.actions.db.DbCompressAction;
import org.openlca.app.navigation.actions.db.DbCopyAction;
import org.openlca.app.navigation.actions.db.DbCreateAction;
import org.openlca.app.navigation.actions.db.DbDeleteAction;
import org.openlca.app.navigation.actions.db.DbExportAction;
import org.openlca.app.navigation.actions.db.DbPropertiesAction;
import org.openlca.app.navigation.actions.db.DbRenameAction;
import org.openlca.app.navigation.actions.db.DbRestoreAction;
import org.openlca.app.navigation.actions.db.DbValidationAction;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.core.model.ModelType;

class DatabaseMenu implements IMenuListener {

	private DatabaseMenu(IMenuManager manager) {
		var menu = new MenuManager(M.Database, "Database.Menu");
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
		menu.add(new DbRestoreAction());
		if (Database.getActiveConfiguration() == null)
			return;
		var checkLinksAction = Actions.create(
				M.CheckLinkingProperties, null, LinkingPropertiesPage::show);
		var actions = new IAction[] {
				new DbExportAction(),
				new DbValidationAction(),
				new DbCopyAction(),
				new DbRenameAction(),
				new DbDeleteAction(),
				new DbCloseAction(),
				checkLinksAction,
				new DbPropertiesAction(),
				new DbCompressAction(),
		};
		for (var action : actions) {
			menu.add(action);
		}

		var contents = new MenuManager();
		contents.setMenuText("Content");

		contents.add(Actions.create(M.Processes,
			Images.descriptor(ModelType.PROCESS), ProcessTable::show));
		contents.add(Actions.create(M.Parameters,
			Images.descriptor(ModelType.PARAMETER), BigParameterTable::show));
		contents.add(Actions.create(M.Flows,
			Images.descriptor(ModelType.FLOW), FlowTable::show));
		contents.add(Actions.create(M.FlowProperties,
			Images.descriptor(ModelType.FLOW_PROPERTY), FlowPropertyTable::show));
		contents.add(Actions.create(M.Units,
			Images.descriptor(ModelType.UNIT), UnitTable::show));
		contents.add(Actions.create(M.Currencies,
			Images.descriptor(ModelType.CURRENCY), CurrencyTable::show));

		menu.add(contents);
	}
}
