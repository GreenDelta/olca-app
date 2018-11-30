package org.openlca.app.util.viewers;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

public abstract class Comparator<T> extends ViewerComparator {

	public final int column;
	public boolean ascending = true;

	protected Comparator(int column) {
		this.column = column;
	}

	protected abstract int compare(T e1, T e2);

	@Override
	@SuppressWarnings("unchecked")
	public final int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 == null && e2 == null)
			return 0;
		if (e1 == null || e2 == null)
			return e1 == null ? -1 : 1;
		int c = compare((T) e1, (T) e2);
		return ascending ? c : -c;
	}
	
}
