package org.openlca.app.util.tables;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

abstract class Sorter<T> extends ViewerSorter {

	protected final int column;
	protected boolean ascending = true;

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
