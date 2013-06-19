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

import java.util.List;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.openlca.core.application.actions.OpenEditorAction;
import org.openlca.core.application.navigation.INavigationElement;
import org.openlca.core.application.navigation.ModelNavigationElement;
import org.openlca.core.application.navigation.NavigationRoot;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.ui.Viewers;

/**
 * Extension of the {@link CommonNavigator}. This navigator shows all registed
 * data providers and there category system
 * 
 * @author Sebastian Greve
 * @author Michael Srocka (made some refactorings)
 */
public class Navigator extends CommonNavigator {

	public static String ID = "org.openlca.core.application.navigator";
	private NavigationRoot root;

	@Override
	protected Object getInitialInput() {
		root = new NavigationRoot();
		return root;
	}

	@Override
	protected void initListeners(TreeViewer viewer) {
		super.initListeners(viewer);
		viewer.setUseHashlookup(true);
		ColumnViewerToolTipSupport.enableFor(viewer);
		viewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				if (!selection.isEmpty()
						&& selection.getFirstElement() instanceof ModelNavigationElement)
					openModel(selection);
			}
		});

	}

	private void openModel(IStructuredSelection selection) {
		ModelNavigationElement element = (ModelNavigationElement) selection
				.getFirstElement();
		IModelComponent modelComponent = (IModelComponent) element.getData();
		OpenEditorAction action = new OpenEditorAction();
		action.setModelComponent(element.getDatabase(), modelComponent);
		action.run();
	}

	/**
	 * Get the root of the navigation tree.
	 */
	public NavigationRoot getRoot() {
		return root;
	}

	/**
	 * Refresh the navigation view if it is available.
	 */
	public static void refresh() {
		CommonViewer viewer = getNavigationViewer();
		if (viewer != null)
			viewer.refresh();
	}

	/**
	 * Refreshes the navigation view and expands the navigation tree to the
	 * given level.
	 */
	public static void refresh(int expandLevel) {
		CommonViewer viewer = getNavigationViewer();
		if (viewer != null)
			viewer.refresh();
		expand(viewer, expandLevel);
	}

	public static void refresh(INavigationElement element) {
		CommonViewer viewer = getNavigationViewer();
		if (viewer == null || element == null)
			return;
		getNavigationViewer().refresh(element);
		getNavigationViewer().expandToLevel(element, 1);
	}

	private static CommonViewer getNavigationViewer() {
		CommonViewer viewer = null;
		Navigator instance = getInstance();
		if (instance != null) {
			viewer = instance.getCommonViewer();
		}
		return viewer;
	}

	private static void expand(CommonViewer viewer, int expandLevel) {
		if (viewer != null)
			viewer.expandToLevel(expandLevel);
	}

	/**
	 * Returns the instance of the navigation view or NULL if there is# no such
	 * instance available.
	 */
	public static Navigator getInstance() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench == null)
			return null;
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		if (window == null)
			return null;
		IWorkbenchPage page = window.getActivePage();
		if (page == null)
			return null;
		IViewPart part = page.findView(Navigator.ID);
		if (part instanceof Navigator)
			return (Navigator) part;
		return null;
	}

	/**
	 * Returns the root of the navigation tree or NULL if there is no such root
	 * available.
	 */
	public static NavigationRoot getNavigationRoot() {
		NavigationRoot root = null;
		Navigator navigator = getInstance();
		if (navigator != null)
			root = navigator.getRoot();
		return root;
	}

	public INavigationElement getFirstSelected() {
		INavigationElement[] all = getAllSelected();
		return all.length > 0 ? all[0] : null;
	}

	public INavigationElement[] getAllSelected() {
		List<INavigationElement> selection = Viewers
				.getAllSelected(getNavigationViewer());
		return selection.toArray(new INavigationElement[selection.size()]);
	}

}
