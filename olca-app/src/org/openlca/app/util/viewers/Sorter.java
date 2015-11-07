package org.openlca.app.util.viewers;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public abstract class Sorter<T> extends ViewerSorter {

	public final int column;
	public boolean ascending = true;

	protected Sorter(int column) {
		this.column = column;
	}

	protected abstract int compare(Object o1, Object o2);

	@Override
	public final int compare(Viewer viewer, Object o1, Object o2) {
		int c = compare(o1, o2);
		return ascending ? c : -c;
	}
}
