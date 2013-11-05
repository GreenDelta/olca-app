package org.openlca.app.util;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public abstract class TableColumnSorter<T> extends ViewerSorter {

	private Class<T> contentType;
	private int column;
	private boolean ascending = true;

	public TableColumnSorter(Class<T> contentType, int column) {
		this.contentType = contentType;
		this.column = column;
	}

	public boolean isAscending() {
		return ascending;
	}

	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	/**
	 * Returns the comparison of the two objects in ascending mode. If the
	 * viewer is set to descending, we invert the result of this method.
	 */
	public abstract int compare(T obj1, T obj2);

	@Override
	public final int compare(Viewer viewer, Object e1, Object e2) {
		if (!contentType.isInstance(e1) || !contentType.isInstance(e2))
			return 0;
		T t1 = contentType.cast(e1);
		T t2 = contentType.cast(e2);
		int c = compare(t1, t2);
		return ascending ? c : -c;
	}

}
