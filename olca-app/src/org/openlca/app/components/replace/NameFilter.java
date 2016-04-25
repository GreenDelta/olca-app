package org.openlca.app.components.replace;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.python.google.common.base.Strings;

class NameFilter extends ViewerFilter {

	String filter;
	
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		BaseDescriptor elem = (BaseDescriptor) element;
		if (Strings.isNullOrEmpty(filter))
			return true;
		if (elem.getName() == null)
			return true;
		if (elem.getName().toLowerCase().contains(filter))
			return true;
		return false;
	}

}