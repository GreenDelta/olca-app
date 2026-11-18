package org.openlca.app.navigation;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.app.navigation.elements.NavigationRoot;
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

	/// Returns the content of the model type folder of the active database:
	/// the root categories and the models without category of that type.
	private static List<INavigationElement<?>> contentOf(ModelType type) {
		var config = Database.getActiveConfiguration();
		if (type == null || config == null)
			return Collections.emptyList();
		var dbElem = new DatabaseElement(new NavigationRoot(), config);
		return new ModelTypeElement(dbElem, type).getChildren();
	}
}
