/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.views.navigator;

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
import org.openlca.core.application.Messages;
import org.openlca.core.application.actions.AddCategoryAction;
import org.openlca.core.application.actions.CopyAction;
import org.openlca.core.application.actions.CreateDatabaseAction;
import org.openlca.core.application.actions.CutAction;
import org.openlca.core.application.actions.DeleteAction;
import org.openlca.core.application.actions.DeleteCategoryAction;
import org.openlca.core.application.actions.DeleteDatabaseAction;
import org.openlca.core.application.actions.ExportDatabaseAction;
import org.openlca.core.application.actions.IImportAction;
import org.openlca.core.application.actions.INavigationAction;
import org.openlca.core.application.actions.ImportDatabaseAction;
import org.openlca.core.application.actions.OpenEditorAction;
import org.openlca.core.application.actions.OpenNewWizardAction;
import org.openlca.core.application.actions.OpenPropertiesAction;
import org.openlca.core.application.actions.OpenUsageAction;
import org.openlca.core.application.actions.PasteAction;
import org.openlca.core.application.actions.RegisterDataProviderAction;
import org.openlca.core.application.actions.RenameCategoryAction;
import org.openlca.core.application.actions.UnregisterDataProviderAction;
import org.openlca.core.application.db.ServerConnectionAction;
import org.openlca.core.application.navigation.CategoryNavigationElement;
import org.openlca.core.application.navigation.DataProviderNavigationElement;
import org.openlca.core.application.navigation.DatabaseNavigationElement;
import org.openlca.core.application.navigation.INavigationElement;
import org.openlca.core.application.navigation.ModelNavigationElement;
import org.openlca.core.application.wizards.MySQLWizard;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.IDatabaseServer;
import org.openlca.core.model.Category;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of the {@link CommonActionProvider} to provide the navigator with
 * actions
 * 
 * @author Sebastian Greve
 * 
 */
public class NavigationActionProvider extends CommonActionProvider {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private ExportActionProvider exportActionProvider = new ExportActionProvider();

	private INavigationAction openUsageAction = new OpenUsageAction();

	@Override
	public void fillContextMenu(IMenuManager menu) {
		ActionContext con = getContext();
		IStructuredSelection selection = (IStructuredSelection) con
				.getSelection();
		if (selection == null || selection.isEmpty())
			menu.add(new RegisterDataProviderAction(new MySQLWizard(),
					Messages.NewMySQLConnection));
		else if (selection.size() > 1)
			multiSelection(menu, selection.toArray());
		else
			singleSelection(menu, selection.getFirstElement());
	}

	private void appendCategoryActions(IMenuManager menu,
			CategoryNavigationElement element) {
		Category category = (Category) element.getData();

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
			if (clazz != null && category.getComponentClass().equals(clazz)) {
				// found wizard
				wizardId = elements[i].getAttribute("newWizardId");
			}
			i++;
		}

		// if a wizard was found
		if (wizardId != null) {
			// add open new wizard action
			menu.add(new OpenNewWizardAction(wizardId, category.getId(),
					element.getDatabase()));
			menu.add(new Separator());

			// if not top category
			if (!category.getId().equals(category.getComponentClass())) {
				// append cut/copy action
				menu.add(new CutAction(new INavigationElement[] { element }));
				menu.add(new CopyAction(new INavigationElement[] { element }));
			}

			// append paste action
			menu.add(new PasteAction(element));
			menu.add(new Separator());
		}

		// append add action
		menu.add(new AddCategoryAction(category, element.getDatabase()));

		// if not top category
		if (!category.getId().equals(category.getComponentClass())) {
			// if element is deletable
			if (element.canBeDeleted()) {
				// add delete action
				menu.add(new DeleteCategoryAction(category, element
						.getDatabase()));
			}
			// add rename action
			menu.add(new RenameCategoryAction(category, element.getDatabase()));
		}
		menu.add(new Separator());
	}

	private void appendDataProviderActions1(IMenuManager menu,
			IDatabaseServer dataProvider) {
		menu.add(new ServerConnectionAction(dataProvider));
		if (dataProvider.isRunning()) {
			menu.add(new CreateDatabaseAction(dataProvider));
			menu.add(new ImportDatabaseAction(dataProvider));
		}
		menu.add(new Separator());
		menu.add(new OpenPropertiesAction(dataProvider));
		menu.add(new UnregisterDataProviderAction(dataProvider));
	}

	/** Appends the available export actions for the model component. */
	private void appendExportActions(IMenuManager menu, IDatabase database,
			IModelComponent model) {
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
			ModelNavigationElement navElem) {
		if (navElem.getDatabase() == null)
			return;
		OpenEditorAction openAction = new OpenEditorAction();
		IModelComponent modelComponent = (IModelComponent) navElem.getData();
		IDatabase database = navElem.getDatabase();
		openAction.setModelComponent(database, modelComponent);
		menu.add(openAction);
		menu.add(new Separator());
		if (!(modelComponent instanceof UnitGroup)) {
			menu.add(new CutAction(new INavigationElement[] { navElem }));
			menu.add(new CopyAction(new INavigationElement[] { navElem }));
		}
		if (openUsageAction.accept(navElem))
			menu.add(openUsageAction);
		menu.add(new DeleteAction(database, modelComponent));
		menu.add(new Separator());
		appendExportActions(menu, database, modelComponent);
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
		OpenEditorAction openAction = new OpenEditorAction();
		INavigationElement[] elements = new INavigationElement[selectedObjects.length];
		IModelComponent[] components = new IModelComponent[selectedObjects.length];
		IDatabase[] databases = new IDatabase[selectedObjects.length];

		// for each selected object
		for (int j = 0; j < selectedObjects.length; j++) {
			// cast
			ModelNavigationElement navElem = (ModelNavigationElement) selectedObjects[j];
			// get model component
			components[j] = (IModelComponent) navElem.getData();
			// store element
			elements[j] = navElem;
			// get database
			databases[j] = navElem.getDatabase();
		}

		openAction.setModelComponents(databases, components);
		menu.add(openAction);
		menu.add(new Separator());
		// add cut action
		menu.add(new CutAction(elements));
		// add copy action
		menu.add(new CopyAction(elements));
		// add delete action
		menu.add(new DeleteAction(databases, components));
		menu.add(new Separator());
	}

	private void appendMultiCategoryActions(IMenuManager menu,
			Object[] selectedObjects) {
		Category[] categories = new Category[selectedObjects.length];
		CategoryNavigationElement[] elements = new CategoryNavigationElement[selectedObjects.length];
		IDatabase[] databases = new IDatabase[selectedObjects.length];
		for (int j = 0; j < categories.length; j++) {
			CategoryNavigationElement navElem = (CategoryNavigationElement) selectedObjects[j];
			categories[j] = (Category) navElem.getData();
			elements[j] = navElem;
			databases[j] = navElem.getDatabase();
		}
		menu.add(new CutAction(elements));
		menu.add(new CopyAction(elements));
		menu.add(new DeleteCategoryAction(categories, databases));
		menu.add(new Separator());
	}

	private void appendDatabaseActions(IMenuManager menu,
			IDatabaseServer dataProvider, IDatabase database) {
		menu.add(new ExportDatabaseAction(dataProvider, database));
		menu.add(new DeleteDatabaseAction(dataProvider, database.getName()));
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
			if (selectedObjects[i] instanceof ModelNavigationElement) {
				allSubCategories = false;
			} else {
				allModelComponents = false;
				if (!(selectedObjects[i] instanceof CategoryNavigationElement)) {
					allSubCategories = false;
				} else {
					Category category = (Category) ((CategoryNavigationElement) selectedObjects[i])
							.getData();
					if (category.getComponentClass().equals(category.getId())) {
						allSubCategories = false;
					}
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
		if (elem instanceof DataProviderNavigationElement) {
			appendDataProviderActions1(menu,
					(IDatabaseServer) ((DataProviderNavigationElement) elem)
							.getData());
		} else if (elem instanceof DatabaseNavigationElement) {
			IDatabaseServer dataProvider = (IDatabaseServer) ((DatabaseNavigationElement) elem)
					.getParent().getData();
			IDatabase database = (IDatabase) ((DatabaseNavigationElement) elem)
					.getData();
			appendDatabaseActions(menu, dataProvider, database);
		} else if (elem instanceof ModelNavigationElement) {
			appendModelComponentActions(menu, (ModelNavigationElement) elem);
		} else if (elem instanceof CategoryNavigationElement) {
			appendCategoryActions(menu, (CategoryNavigationElement) elem);
		}
		menu.add(new RegisterDataProviderAction(new MySQLWizard(),
				Messages.NewMySQLConnection));
	}
}
