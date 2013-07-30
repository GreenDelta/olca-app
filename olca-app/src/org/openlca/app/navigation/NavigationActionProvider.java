/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.navigation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.openlca.app.navigation.actions.ActivateDatabaseAction;
import org.openlca.app.navigation.actions.CloseDatabaseAction;
import org.openlca.app.navigation.actions.CreateCategoryAction;
import org.openlca.app.navigation.actions.CreateDatabaseAction;
import org.openlca.app.navigation.actions.CreateModelAction;
import org.openlca.app.navigation.actions.DeleteCategoryAction;
import org.openlca.app.navigation.actions.DeleteDatabaseAction;
import org.openlca.app.navigation.actions.DeleteModelAction;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.actions.OpenUsageAction;
import org.openlca.app.navigation.actions.RenameCategoryAction;
import org.openlca.core.application.Messages;
import org.openlca.core.application.actions.CopyAction;
import org.openlca.core.application.actions.CutAction;
import org.openlca.core.application.actions.ExportDatabaseAction;
import org.openlca.core.application.actions.IImportAction;
import org.openlca.core.application.actions.ImportDatabaseAction;
import org.openlca.core.application.actions.OpenPropertiesAction;
import org.openlca.core.application.actions.PasteAction;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.IDatabaseServer;
import org.openlca.core.model.Category;
import org.openlca.core.model.RootEntity;
import org.openlca.ui.Viewers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds the actions to the context menu of the navigation tree.
 */
public class NavigationActionProvider extends CommonActionProvider {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private ExportActionProvider exportActionProvider = new ExportActionProvider();

	//@formatter:off
	private INavigationAction[][] actions = new INavigationAction[][] {
			// database actions
			new INavigationAction[] {
				new ActivateDatabaseAction(), 
				new CloseDatabaseAction(), 
				new DeleteDatabaseAction()
			},			
			// model actions
			new INavigationAction[] {
				new CreateModelAction(),
				new OpenUsageAction(),				
				new DeleteModelAction()
			},			
			// category actions
			new INavigationAction[] {
				new CreateCategoryAction(),
				new RenameCategoryAction(),
				new DeleteCategoryAction()		
			},			
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
		menu.add(new CreateDatabaseAction());
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

	private void appendCategoryActions(IMenuManager menu,
			CategoryElement element) {
		Category category = element.getContent();

		String wizardId = null;

		// get the editors
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry
				.getConfigurationElementsFor("org.openlca.core.application.editors");
		int i = 0;

		// for each editor while not found a matching one
		while (wizardId == null && i < elements.length) {
			String clazz = elements[i].getAttribute("componentClass");
			// if matching
			if (clazz != null) {
				// found wizard
				wizardId = elements[i].getAttribute("newWizardId");
			}
			i++;
		}

		if (wizardId != null) {
			menu.add(new Separator());
			menu.add(new CutAction(new INavigationElement[] { element }));
			menu.add(new CopyAction(new INavigationElement[] { element }));
			menu.add(new PasteAction(element));
			menu.add(new Separator());
		}

		menu.add(new Separator());
	}

	private void appendDataProviderActions1(IMenuManager menu,
			IDatabaseServer dataProvider) {
		if (dataProvider.isRunning()) {
			menu.add(new ImportDatabaseAction(dataProvider));
		}
		menu.add(new Separator());
		menu.add(new OpenPropertiesAction(dataProvider));
	}

	/** Appends the available export actions for the model component. */
	private void appendExportActions(IMenuManager menu, IDatabase database,
			RootEntity model) {
		List<IAction> actions = exportActionProvider.getFor(model, database);
		if (actions.isEmpty())
			return;
		IMenuManager parent = menu;
		if (actions.size() > 1) {
			parent = new MenuManager(Messages.Export);
			menu.add(parent);
		}
		for (IAction action : actions)
			parent.add(action);
		menu.add(new Separator());
	}

	/**
	 * Appends the import popup menu
	 * 
	 * @param menu
	 *            The menu manager
	 * @param database
	 *            The database
	 */
	private void appendImportPopupMenu(IMenuManager menu, IDatabase database) {
		List<IAction> actions = new ArrayList<>();
		// load import actions from extension registry
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry
				.getConfigurationElementsFor("org.openlca.core.application.importAction");

		// for each found configuration element
		for (IConfigurationElement element : elements) {
			try {
				// cast import action
				IImportAction importAction = (IImportAction) element
						.createExecutableExtension("importAction");
				if (importAction != null) {
					importAction.setDatabase(database);
					actions.add(importAction);
				}
			} catch (CoreException e) {
				log.error("Append import popup menu failed", e);
			}
		}

		// if only one import action was found
		if (actions.size() == 1) {
			// append action directly
			menu.add(actions.get(0));
		} else {
			// create sub menu
			IMenuManager menuManager = new MenuManager(Messages.Import);

			// for each import action
			for (IAction action : actions) {
				// append
				menuManager.add(action);
			}

			// append sub menu
			menu.add(menuManager);
		}
		if (actions.size() > 0) {
			menu.add(new Separator());
		}
	}

	/**
	 * Appends the model component actions
	 * 
	 * @param menu
	 *            The menu manager
	 * @param navElem
	 *            The model component element to append the actions to
	 */
	private void appendModelComponentActions(IMenuManager menu,
			ModelElement navElem) {
		// OpenEditorAction openAction = new OpenEditorAction();
		// RootEntity modelComponent = (RootEntity) navElem.getData();
		// IDatabase database = navElem.getDatabase();
		// openAction.setModelComponent(database, modelComponent);
		// menu.add(openAction);
		// menu.add(new Separator());
		// if (!(modelComponent instanceof UnitGroup)) {
		// menu.add(new CutAction(new INavigationElement[] { navElem }));
		// menu.add(new CopyAction(new INavigationElement[] { navElem }));
		// }
		// menu.add(new DeleteAction(database, modelComponent));
		// menu.add(new Separator());
		// appendExportActions(menu, database, modelComponent);
	}

	/**
	 * Append the model component actions which apply to multi selection
	 * 
	 * @param menu
	 *            The menu manager
	 * @param selectedObjects
	 *            The selected elements
	 */
	private void appendMulitModelComponentActions(IMenuManager menu,
			Object[] selectedObjects) {
		// OpenEditorAction openAction = new OpenEditorAction();
		// INavigationElement[] elements = new
		// INavigationElement[selectedObjects.length];
		// IModelComponent[] components = new
		// IModelComponent[selectedObjects.length];
		// IDatabase[] databases = new IDatabase[selectedObjects.length];
		//
		// // for each selected object
		// for (int j = 0; j < selectedObjects.length; j++) {
		// // cast
		// ModelElement navElem = (ModelElement) selectedObjects[j];
		// // get model component
		// components[j] = (IModelComponent) navElem.getData();
		// // store element
		// elements[j] = navElem;
		// // get database
		// databases[j] = navElem.getDatabase();
		// }
		//
		// openAction.setModelComponents(databases, components);
		// menu.add(openAction);
		// menu.add(new Separator());
		// // add cut action
		// menu.add(new CutAction(elements));
		// // add copy action
		// menu.add(new CopyAction(elements));
		// // add delete action
		// menu.add(new DeleteAction(databases, components));
		// menu.add(new Separator());
	}

	private void appendMultiCategoryActions(IMenuManager menu,
			Object[] selectedObjects) {
		Category[] categories = new Category[selectedObjects.length];
		CategoryElement[] elements = new CategoryElement[selectedObjects.length];
		IDatabase[] databases = new IDatabase[selectedObjects.length];
		for (int j = 0; j < categories.length; j++) {
			CategoryElement navElem = (CategoryElement) selectedObjects[j];
			categories[j] = navElem.getContent();
			elements[j] = navElem;
		}
		menu.add(new CutAction(elements));
		menu.add(new CopyAction(elements));
		menu.add(new Separator());
	}

	private void appendDatabaseActions(IMenuManager menu,
			IDatabaseServer dataProvider, IDatabase database) {
		menu.add(new ExportDatabaseAction(dataProvider, database));
		menu.add(new Separator());
		appendImportPopupMenu(menu, database);
	}

	/**
	 * Creates menu for multi selection
	 * 
	 * @param menu
	 *            The menu manager
	 * @param selectedObjects
	 *            The selected elements
	 */
	private void multiSelection(IMenuManager menu, Object[] selectedObjects) {
		// add actions for multi selection
		boolean allModelComponents = true;
		boolean allSubCategories = true;
		int i = 0;
		// check if all selected objects are whether model components or sub
		// categories of the top level categories
		while ((allModelComponents || allSubCategories)
				&& i < selectedObjects.length) {
			if (selectedObjects[i] instanceof ModelElement) {
				allSubCategories = false;
			} else {
				allModelComponents = false;
				if (!(selectedObjects[i] instanceof CategoryElement)) {
					allSubCategories = false;
				} else {
					allSubCategories = false;
				}
			}
			i++;
		}
		if (allModelComponents) {
			appendMulitModelComponentActions(menu, selectedObjects);
		} else if (allSubCategories) {
			appendMultiCategoryActions(menu, selectedObjects);
		}
	}

	private void singleSelection(IMenuManager menu, Object elem) {
		if (elem instanceof ModelElement) {
			appendModelComponentActions(menu, (ModelElement) elem);
		} else if (elem instanceof CategoryElement) {
			appendCategoryActions(menu, (CategoryElement) elem);
		}
	}
}
