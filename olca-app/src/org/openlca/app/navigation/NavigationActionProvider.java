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

import java.util.List;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.openlca.app.navigation.actions.ActivateDatabaseAction;
import org.openlca.app.navigation.actions.CloseDatabaseAction;
import org.openlca.app.navigation.actions.CopyAction;
import org.openlca.app.navigation.actions.CreateCategoryAction;
import org.openlca.app.navigation.actions.CreateDatabaseAction;
import org.openlca.app.navigation.actions.CreateModelAction;
import org.openlca.app.navigation.actions.CutAction;
import org.openlca.app.navigation.actions.DeleteCategoryAction;
import org.openlca.app.navigation.actions.DeleteDatabaseAction;
import org.openlca.app.navigation.actions.DeleteModelAction;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.actions.OpenUsageAction;
import org.openlca.app.navigation.actions.PasteAction;
import org.openlca.app.navigation.actions.RenameCategoryAction;
import org.openlca.app.util.Viewers;

/**
 * Adds the actions to the context menu of the navigation tree.
 */
public class NavigationActionProvider extends CommonActionProvider {

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
			// transfer actions
			new INavigationAction[] {
				new CutAction(),
				new CopyAction(),				
				new PasteAction()
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

}
