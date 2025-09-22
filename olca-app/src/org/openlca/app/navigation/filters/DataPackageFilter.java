package org.openlca.app.navigation.filters;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.ModelElement;

/**
 * Show only categories with the model types that are passed to this filter.
 */
public class DataPackageFilter extends ViewerFilter {

	@Override
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof ModelElement e)
			return !e.getDataPackage().isPresent();
		if (element instanceof CategoryElement e)
			return e.hasNonDataPackageContent();
		return true;
	}
}
