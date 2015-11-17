package org.openlca.app.navigation;

import java.util.List;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.openlca.app.App;
import org.openlca.app.navigation.actions.CopyAction;
import org.openlca.app.navigation.actions.CreateCategoryAction;
import org.openlca.app.navigation.actions.CreateModelAction;
import org.openlca.app.navigation.actions.CutAction;
import org.openlca.app.navigation.actions.DatabaseActivateAction;
import org.openlca.app.navigation.actions.DatabaseCloseAction;
import org.openlca.app.navigation.actions.DatabaseCopyAction;
import org.openlca.app.navigation.actions.DatabaseCreateAction;
import org.openlca.app.navigation.actions.DatabaseDeleteAction;
import org.openlca.app.navigation.actions.DatabaseExportAction;
import org.openlca.app.navigation.actions.DatabaseImportAction;
import org.openlca.app.navigation.actions.DatabasePropertiesAction;
import org.openlca.app.navigation.actions.DatabaseRenameAction;
import org.openlca.app.navigation.actions.DeleteCategoryAction;
import org.openlca.app.navigation.actions.DeleteModelAction;
import org.openlca.app.navigation.actions.ExportAction;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.actions.ImportAction;
import org.openlca.app.navigation.actions.ImportKmlAction;
import org.openlca.app.navigation.actions.ImportXmlKmlAction;
import org.openlca.app.navigation.actions.OpenModelAction;
import org.openlca.app.navigation.actions.OpenUsageAction;
import org.openlca.app.navigation.actions.PasteAction;
import org.openlca.app.navigation.actions.RenameCategoryAction;
import org.openlca.app.navigation.actions.XEI3MarketProcessCleanUp;
import org.openlca.app.navigation.actions.XEI3MetaDataImportAction;
import org.openlca.app.navigation.actions.XNexusIndexExportAction;
import org.openlca.app.navigation.actions.XParameterCheckAction;
import org.openlca.app.navigation.actions.XRefDataExport;
import org.openlca.app.navigation.actions.XRefDataImport;
import org.openlca.app.navigation.actions.cloud.CommitAction;
import org.openlca.app.navigation.actions.cloud.ConnectAction;
import org.openlca.app.navigation.actions.cloud.DeleteAction;
import org.openlca.app.navigation.actions.cloud.DisconnectAction;
import org.openlca.app.navigation.actions.cloud.FetchAction;
import org.openlca.app.navigation.actions.cloud.ShareAction;
import org.openlca.app.navigation.actions.cloud.UnshareAction;
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
			new CommitAction(), new FetchAction() 
		},
		new INavigationAction[]	{ 
			new ShareAction(), new UnshareAction() 
		},
		new INavigationAction[]	{ 
			new ConnectAction(), new DeleteAction(), new DisconnectAction() 
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
		menu.add(new DatabaseCreateAction());
		menu.add(new DatabaseImportAction());
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
		if (registered == 0)
			return;
		menu.add(subMenu);
		menu.add(new Separator());
	}
	
	private INavigationAction[] getDatabaseActions() {
		int count = App.runsInDevMode() ? 13 : 7;
		INavigationAction[] actions = new INavigationAction[count];
		actions[0] = new DatabaseActivateAction();
		actions[1] = new DatabaseCopyAction();
		actions[2] = new DatabasePropertiesAction();
		actions[3] = new DatabaseCloseAction();
		actions[4] = new DatabaseExportAction();
		actions[5] = new DatabaseRenameAction();
		actions[6] = new DatabaseDeleteAction();
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
