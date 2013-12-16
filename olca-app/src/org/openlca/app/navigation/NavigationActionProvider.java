package org.openlca.app.navigation;

import java.util.List;

import org.eclipse.jface.action.IMenuManager;
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
import org.openlca.app.navigation.actions.DatabaseCreateAction;
import org.openlca.app.navigation.actions.DatabaseDeleteAction;
import org.openlca.app.navigation.actions.DatabaseExportAction;
import org.openlca.app.navigation.actions.DatabaseImportAction;
import org.openlca.app.navigation.actions.DatabasePropertiesAction;
import org.openlca.app.navigation.actions.DeleteCategoryAction;
import org.openlca.app.navigation.actions.DeleteModelAction;
import org.openlca.app.navigation.actions.ExportAction;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.actions.ImportAction;
import org.openlca.app.navigation.actions.OpenModelAction;
import org.openlca.app.navigation.actions.OpenUsageAction;
import org.openlca.app.navigation.actions.PasteAction;
import org.openlca.app.navigation.actions.RenameCategoryAction;
import org.openlca.app.navigation.actions.XEI3MarketProcessCleanUp;
import org.openlca.app.navigation.actions.XEI3MetaDataImportAction;
import org.openlca.app.util.Viewers;

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
			// category actions
			new INavigationAction[] {
				new CreateCategoryAction(),
				new RenameCategoryAction(),
				new DeleteCategoryAction()		
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
			registered += registerSingleActions(elements.get(0), menu);
		else if (elements.size() > 1)
			registered += registerMultiActions(elements, menu);
		if (registered > 0)
			menu.add(new Separator());
		menu.add(new DatabaseCreateAction());
		menu.add(new DatabaseImportAction());
	}

	private INavigationAction[] getDatabaseActions() {
		int count = App.runsInDevMode() ? 7 : 5;
		INavigationAction[] actions = new INavigationAction[count];
		actions[0] = new DatabaseActivateAction();
		actions[1] = new DatabasePropertiesAction();
		actions[2] = new DatabaseCloseAction();
		actions[3] = new DatabaseExportAction();
		actions[4] = new DatabaseDeleteAction();
		if (count == 7) {
			actions[5] = new XEI3MetaDataImportAction();
			actions[6] = new XEI3MarketProcessCleanUp();
		}
		return actions;
	}

	private int registerSingleActions(INavigationElement<?> element,
			IMenuManager menu) {
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
			IMenuManager menu) {
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
