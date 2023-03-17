package org.openlca.app.navigation;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.core.model.ModelType;

/**
 * Factory methods for creating navigation trees in the application.
 */
public class NavigationTree {

	/**
	 * Creates a tree viewer for the selection of single models of the given type.
	 * The input of the respective navigation element is already done in this
	 * method.
	 */
	public static TreeViewer forSingleSelection(Composite parent, ModelType type) {
		TreeViewer viewer = createViewer(parent, SWT.SINGLE);
		viewer.setInput(contentOf(type));
		return viewer;
	}

	/**
	 * Creates a tree viewer for the selection of multiple models of the given type.
	 * The input of the respective navigation element is already done in this
	 * method.
	 */
	public static TreeViewer forMultiSelection(Composite parent, ModelType type) {
		TreeViewer viewer = createViewer(parent, SWT.MULTI);
		viewer.setInput(contentOf(type));
		return viewer;
	}

	private static TreeViewer createViewer(Composite parent, int selection) {
		var viewer = new TreeViewer(parent, SWT.BORDER | selection);
		viewer.setContentProvider(new NavigationContentProvider());
		viewer.setLabelProvider(NavigationLabelProvider.withoutRepositoryState());
		viewer.setComparator(new NavigationComparator());
		ColumnViewerToolTipSupport.enableFor(viewer);
		return viewer;
	}

	private static List<INavigationElement<?>> contentOf(ModelType type) {
		if (type == null)
			return Collections.emptyList();
		var root = Navigator.getNavigationRoot();
		if (root == null)
			return Collections.emptyList();
		var queue = new ArrayDeque<INavigationElement<?>>();
		queue.add(root);
		ModelTypeElement elem = null;
		while (!queue.isEmpty()) {
			var next = queue.poll();
			if (next instanceof ModelTypeElement e) {
				if (e.getContent() == type) {
					elem = e;
					break;
				}
				continue;
			}
			queue.addAll(next.getChildren());
		}
		return elem == null
			? Collections.emptyList()
			: elem.getChildren();
	}
}
