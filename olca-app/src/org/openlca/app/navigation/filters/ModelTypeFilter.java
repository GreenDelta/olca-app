package org.openlca.app.navigation.filters;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.openlca.app.navigation.ModelTypeElement;
import org.openlca.core.model.ModelType;

/**
 * Show only categories with the model types that are passed to this filter.
 */
public class ModelTypeFilter extends ViewerFilter {

	private ModelType[] types;

	public ModelTypeFilter(ModelType... types) {
		this.types = types;
	}

	@Override
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (!(element instanceof ModelTypeElement))
			return true;
		if (types == null)
			return false;
		ModelTypeElement e = (ModelTypeElement) element;
		for (ModelType type : types) {
			if (e.getContent() == type)
				return true;
		}
		return false;
	}
}