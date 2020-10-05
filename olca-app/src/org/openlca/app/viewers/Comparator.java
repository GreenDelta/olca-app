package org.openlca.app.viewers;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

public abstract class Comparator<T> extends ViewerComparator {

	public final int column;
	public boolean ascending = true;

	protected Comparator(int column) {
		this.column = column;
	}

	public static <T> Comparator<T> on(int column, java.util.Comparator<T> fn) {
		return new Comparator<>(column) {
			@Override
			protected int compare(T e1, T e2) {
				return fn.compare(e1, e2);
			}
		};
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
