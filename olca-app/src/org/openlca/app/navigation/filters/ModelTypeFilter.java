package org.openlca.app.navigation.filters;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.core.model.ModelType;

/**
 * Show only categories with the model types that are passed to this filter.
 */
public class ModelTypeFilter extends ViewerFilter {

	private final ModelType[] types;

	public ModelTypeFilter(ModelType... types) {
		this.types = types;
	}

	@Override
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (types == null)
			return false;
		if (!(element instanceof ModelTypeElement e))
			return true;
		for (ModelType type : types) {
			if (e.getContent() == type)
				return true;
		}
		return false;
	}
}
