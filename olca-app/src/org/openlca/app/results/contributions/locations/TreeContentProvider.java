package org.openlca.app.results.contributions.locations;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionItem;

class TreeContentProvider implements ITreeContentProvider {

	@Override
	@SuppressWarnings("unchecked")
	public Object[] getElements(Object obj) {
		if (obj == null)
			return new Object[0];
		List<LocationItem> items = List.class.cast(obj);
		return items.toArray(new LocationItem[items.size()]);
	}

	@Override
	public Object[] getChildren(Object parent) {
		if (!(parent instanceof LocationItem))
			return new Object[0];
		LocationItem e = (LocationItem) parent;
		List<ContributionItem<ProcessDescriptor>> items = e.processContributions;
		return items.toArray(new ContributionItem[items.size()]);
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object obj) {
		if (!(obj instanceof LocationItem))
			return false;
		LocationItem element = (LocationItem) obj;
		return element.processContributions.size() > 0;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object old, Object newInput) {
	}

}