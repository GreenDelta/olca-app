package org.openlca.app.navigation;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.openlca.core.model.ModelType;

/**
 * Factory methods for creating navigation trees in the application.
 */
public class NavigationTree {

	/**
	 * Creates a tree viewer with the same content provider, label provider, etc. as
	 * in the navigation tree. This this viewer accepts an instance
	 * {@link INavigationElement} as input.
	 */
	public static TreeViewer createViewer(Composite parent) {
		return createViewer(parent, SWT.SINGLE);
	}

	/**
	 * Creates a tree viewer for the selection of single models of the given type.
	 * The input of the respective navigation element is already done in this
	 * method.
	 */
	public static TreeViewer forSingleSelection(Composite parent, ModelType type) {
		TreeViewer viewer = createViewer(parent, SWT.SINGLE);
		// viewer.setInput(Navigator.findElement(type));
		viewer.setInput(Navigator.findModelRoots(type));
		return viewer;
	}

	/**
	 * Creates a tree viewer for the selection of multiple models of the given type.
	 * The input of the respective navigation element is already done in this
	 * method.
	 */
	public static TreeViewer forMultiSelection(Composite parent, ModelType type) {
		TreeViewer viewer = createViewer(parent, SWT.MULTI);
//		viewer.setInput(Navigator.findElement(type));
		viewer.setInput(Navigator.findModelRoots(type));
		return viewer;
	}

	private static TreeViewer createViewer(Composite parent, int selection) {
		var viewer = new TreeViewer(parent, SWT.BORDER | selection);
		viewer.setContentProvider(new NavigationContentProvider());
		viewer.setLabelProvider(new NavigationLabelProvider(false));
		viewer.setComparator(new NavigationComparator());
		ColumnViewerToolTipSupport.enableFor(viewer);
		return viewer;
	}

}
