package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.cloud.ui.preferences.CloudPreference;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.actions.cloud.CommitAction;
import org.openlca.app.navigation.actions.cloud.ConnectAction;
import org.openlca.app.navigation.actions.cloud.DisconnectAction;
import org.openlca.app.navigation.actions.cloud.FetchAction;
import org.openlca.app.navigation.actions.cloud.OpenSyncViewAction;
import org.openlca.app.navigation.actions.cloud.RebuildIndexAction;
import org.openlca.app.navigation.actions.cloud.ShowCommentsAction;
import org.openlca.app.navigation.actions.cloud.ShowInHistoryAction;
import org.openlca.app.navigation.actions.db.DbActivateAction;
import org.openlca.app.navigation.actions.db.DbCloseAction;
import org.openlca.app.navigation.actions.db.DbCopyAction;
import org.openlca.app.navigation.actions.db.DbCreateAction;
import org.openlca.app.navigation.actions.db.DbDeleteAction;
import org.openlca.app.navigation.actions.db.DbExportAction;
import org.openlca.app.navigation.actions.db.DbImportAction;
import org.openlca.app.navigation.actions.db.DbRenameAction;
import org.openlca.app.util.viewers.Viewers;

/**
 * Adds the actions to the context menu of the navigation tree.
 */
public class NavigationActionProvider extends CommonActionProvider {

	private INavigationAction[][] actions = new INavigationAction[][] {

			getDatabaseActions(),

			// model actions
			new INavigationAction[] {
					new OpenModelAction(),
					new CalculateSystemAction(),
					new CreateModelAction(),
					new OpenUsageAction(),
					new DeleteModelAction(),
					ValidateAction.forModel()
			},
			// transfer actions
			new INavigationAction[] {
					new CutAction(),
					new CopyAction(),
					new PasteAction()
			},
			// IO actions
			new INavigationAction[] {
					new ImportAction(),
					new ExportAction(),
			},
			// Location KML import actions
			new INavigationAction[] {
					new ImportKmlAction(),
					new ImportXmlKmlAction()
			},
			// category actions
			new INavigationAction[] {
					new CreateCategoryAction(),
					new RenameAction()
			}
	};

	private INavigationAction[][] cloudActions = new INavigationAction[][] {
			new INavigationAction[] {
					new CommitAction(),
					new FetchAction(),
					new ShowCommentsAction(),
					new ShowInHistoryAction()
			},
			new INavigationAction[] {
					new RebuildIndexAction(),
					new ConnectAction(),
					new DisconnectAction()
			}
	};

	private INavigationAction[][] cloudCompareActions = new INavigationAction[][] {
			new INavigationAction[] {
					new OpenSyncViewAction(false),
					new OpenSyncViewAction(true)
			}
	};

	@Override
	public void fillContextMenu(IMenuManager menu) {
		ActionContext con = getContext();
		IStructuredSelection selection = (IStructuredSelection) con
				.getSelection();
		List<INavigationElement<?>> elements = Viewers.getAll(selection);
		if (showDbCreate(elements)) {
			menu.add(new DbCreateAction());
			menu.add(new DbImportAction());
		}
		if (elements.size() == 1) {
			registerSingleActions(elements.get(0), menu, actions);
		} else if (elements.size() > 1) {
			registerMultiActions(elements, menu, actions);
		}
		addCloudMenu(elements, menu);
	}

	private boolean showDbCreate(List<INavigationElement<?>> elements) {
		if (elements.isEmpty())
			return true;
		if (elements.size() > 1)
			return false;
		INavigationElement<?> e = elements.get(0);
		return e instanceof DatabaseElement;
	}

	private void addCloudMenu(List<INavigationElement<?>> elements, IMenuManager menu) {
		if (!CloudPreference.doEnable())
			return;
		int registered = 0;
		IMenuManager subMenu = new MenuManager(M.Repository);
		if (elements.size() == 1)
			registered += registerSingleActions(elements.get(0), subMenu, cloudActions);
		else if (elements.size() > 1)
			registered += registerMultiActions(elements, subMenu, cloudActions);
		int subRegistered = 0;
		IMenuManager compareMenu = new MenuManager(M.CompareWith);
		if (elements.size() == 1)
			subRegistered += registerSingleActions(elements.get(0), compareMenu, cloudCompareActions);
		else if (elements.size() > 1)
			subRegistered += registerMultiActions(elements, compareMenu, cloudCompareActions);
		if (subRegistered > 0) {
			subMenu.add(compareMenu);
			registered++;
		}
		if (registered == 0)
			return;
		menu.add(subMenu);
		menu.add(new Separator());
	}

	private INavigationAction[] getDatabaseActions() {
		int count = App.runsInDevMode() ? 13 : 7;
		INavigationAction[] actions = new INavigationAction[count];
		actions[0] = new DbExportAction();
		actions[1] = new DbActivateAction();
		actions[2] = ValidateAction.forDatabase();
		actions[3] = new DbCopyAction();
		actions[4] = new DbRenameAction();
		actions[5] = new DbDeleteAction();
		actions[6] = new DbCloseAction();
		if (App.runsInDevMode()) {
			actions[7] = new XEI3MetaDataImportAction();
			actions[8] = new XEI3MarketProcessCleanUp();
			actions[9] = new XParameterCheckAction();
			actions[10] = new XNexusIndexExportAction();
			actions[11] = new XRefDataExport();
			actions[12] = new XRefDataImport();
		}
		return actions;
	}

	private int registerSingleActions(INavigationElement<?> element,
			IMenuManager menu, INavigationAction[][] actions) {
		int count = 0;
		for (INavigationAction[] group : actions) {
			boolean acceptedOne = false;
			for (INavigationAction action : group)
				if (action.accept(element)) {
					menu.add(action);
					count++;
					acceptedOne = true;
				}
			if (acceptedOne)
				menu.add(new Separator());
		}
		return count;
	}

	private int registerMultiActions(List<INavigationElement<?>> elements,
			IMenuManager menu, INavigationAction[][] actions) {
		int count = 0;
		for (INavigationAction[] group : actions) {
			boolean acceptedOne = false;
			for (INavigationAction action : group)
				if (action.accept(elements)) {
					menu.add(action);
					count++;
					acceptedOne = true;
				}
			if (acceptedOne)
				menu.add(new Separator());
		}
		return count;
	}

}
