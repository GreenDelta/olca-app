package org.openlca.app.navigation.filters;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;

public class NoLibraryDataFilter extends ViewerFilter  {

	@Override
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof ModelElement e)
			return !e.isFromLibrary();
		if (element instanceof CategoryElement e)
			return e.hasNonLibraryContent();
		if (element instanceof INavigationElement<?> e) {
			for (var child : e.getChildren()) {
				if (select(viewer, element, child))
					return true;
			}
			return false;
		}
		return true;
	}

}
