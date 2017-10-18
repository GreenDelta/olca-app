package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.openlca.app.App;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.actions.db.DbActivateAction;
import org.openlca.app.navigation.actions.db.DbCloseAction;
import org.openlca.app.navigation.actions.db.DbCopyAction;
import org.openlca.app.navigation.actions.db.DbCreateAction;
import org.openlca.app.navigation.actions.db.DbDeleteAction;
import org.openlca.app.navigation.actions.db.DbExportAction;
import org.openlca.app.navigation.actions.db.DbImportAction;
import org.openlca.app.navigation.actions.db.DbPropertiesAction;
import org.openlca.app.navigation.actions.db.DbRenameAction;
import org.openlca.app.navigation.actions.db.DbValidateAction;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.util.viewers.Viewers;

/**
 * Adds the actions to the context menu of the navigation tree.
 */
public class NavigationActionProvider extends CommonActionProvider {

	//@formatter:off
	private INavigationAction[][] actions = new INavigationAction[][] {
			
			getDatabaseActions(),
			
			// model actions
			new INavigationAction[] {
				new OpenModelAction(),
				new CalculateSystemAction(),
				new CreateModelAction(),
				new OpenUsageAction(),				
				new DeleteModelAction()
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
				new RenameCategoryAction(),
				new DeleteCategoryAction()		
			}	
	};
	
	private INavigationAction[][] cloudActions = new INavigationAction[][] {	
		new INavigationAction[] { 
			new CloudCommitAction(), 
			new CloudFetchAction() ,
			new CloudOpenHistoryViewAction()
		},
		new INavigationAction[]	{ 
			new CloudConnectAction(), 
			new CloudDisconnectAction() 
		}	
	};
	
	private INavigationAction[][] cloudCompareActions = new INavigationAction[][] {	
		new INavigationAction[] {
			new CloudOpenSyncViewAction(false),			
			new CloudOpenSyncViewAction(true)
		}
	};
	//@formatter:on

	@Override
	public void fillContextMenu(IMenuManager menu) {
		ActionContext con = getContext();
		IStructuredSelection selection = (IStructuredSelection) con
				.getSelection();
		List<INavigationElement<?>> elements = Viewers.getAll(selection);
		int registered = 0;
		if (elements.size() == 1)
			registered += registerSingleActions(elements.get(0), menu, actions);
		else if (elements.size() > 1)
			registered += registerMultiActions(elements, menu, actions);
		if (registered > 0)
			menu.add(new Separator());
		addCloudMenu(elements, menu);
		menu.add(new DbCreateAction());
		menu.add(new DbImportAction());
	}

	private void addCloudMenu(List<INavigationElement<?>> elements, IMenuManager menu) {
		if (!FeatureFlag.REPOSITORIES.isEnabled())
			return;
		int registered = 0;
		IMenuManager subMenu = new MenuManager("Repository");
		if (elements.size() == 1)
			registered += registerSingleActions(elements.get(0), subMenu, cloudActions);
		else if (elements.size() > 1)
			registered += registerMultiActions(elements, subMenu, cloudActions);
		int subRegistered = 0;
		IMenuManager compareMenu = new MenuManager("Compare with");
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
		int count = App.runsInDevMode() ? 15 : 9;
		INavigationAction[] actions = new INavigationAction[count];
		actions[0] = new DbActivateAction();
		actions[1] = new DbCopyAction();
		actions[2] = new DbPropertiesAction();
		actions[3] = new DbValidateAction();
		actions[4] = new DbCloseAction();
		actions[5] = new DbExportAction();
		actions[6] = new DbRenameAction();
		actions[7] = new DbDeleteAction();
		actions[8] = new OpenUpdateManagerAction();
		if (App.runsInDevMode()) {
			actions[9] = new XEI3MetaDataImportAction();
			actions[10] = new XEI3MarketProcessCleanUp();
			actions[11] = new XParameterCheckAction();
			actions[12] = new XNexusIndexExportAction();
			actions[13] = new XRefDataExport();
			actions[14] = new XRefDataImport();
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
