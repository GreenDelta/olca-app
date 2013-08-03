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

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.openlca.app.App;
import org.openlca.app.util.Viewers;
import org.openlca.core.model.descriptors.BaseDescriptor;

import com.google.common.base.Objects;

/**
 * The navigation tree.
 */
public class Navigator extends CommonNavigator {

	public static String ID = "views.navigation";
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
				openModel(event.getSelection());
			}
		});
	}

	private void openModel(ISelection selection) {
		Object element = Viewers.getFirst(selection);
		if (!(element instanceof ModelElement))
			return;
		ModelElement e = (ModelElement) element;
		BaseDescriptor d = e.getContent();
		App.openEditor(d);
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
		NavigationRoot root = getNavigationRoot();
		if (viewer != null && root != null) {
			root.update();
			viewer.refresh();
			viewer.expandToLevel(2);
		}
	}

	public static void refresh(INavigationElement<?> element) {
		CommonViewer viewer = getNavigationViewer();
		if (viewer == null || element == null)
			return;
		element.update();
		getNavigationViewer().refresh(element);
	}

	public static void select(Object element) {
		if (element == null)
			return;
		Navigator instance = getInstance();
		if (instance == null)
			return;
		INavigationElement<?> navElement = findElement(element);
		if (navElement == null)
			return;

		instance.selectReveal(new StructuredSelection(navElement));
	}

	private static CommonViewer getNavigationViewer() {
		CommonViewer viewer = null;
		Navigator instance = getInstance();
		if (instance != null) {
			viewer = instance.getCommonViewer();
		}
		return viewer;
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
		IViewPart part = page.findView(ID);
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

	/**
	 * Returns the navigation element with the given content if it exists.
	 */
	public static INavigationElement<?> findElement(Object content) {
		NavigationRoot root = getNavigationRoot();
		if (content == null || root == null)
			return null;
		Queue<INavigationElement<?>> queue = new LinkedList<>();
		queue.add(root);
		while (!queue.isEmpty()) {
			INavigationElement<?> next = queue.poll();
			if (Objects.equal(next.getContent(), content))
				return next;
			queue.addAll(next.getChildren());
		}
		return null;
	}

	public INavigationElement<?> getFirstSelected() {
		INavigationElement<?>[] all = getAllSelected();
		return all.length > 0 ? all[0] : null;
	}

	public INavigationElement<?>[] getAllSelected() {
		List<INavigationElement<?>> selection = Viewers
				.getAllSelected(getNavigationViewer());
		return selection.toArray(new INavigationElement[selection.size()]);
	}

}
