package org.openlca.app.util;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.openlca.util.Strings;

public class TableColumnSorter<T> extends ViewerSorter {

	private Class<T> contentType;
	private int column;
	private boolean ascending = true;
	private ITableLabelProvider labelProvider;

	public TableColumnSorter(Class<T> contentType, int column) {
		this(contentType, column, null);
	}

	public TableColumnSorter(Class<T> contentType, int column,
			ITableLabelProvider labelProvider) {
		this.contentType = contentType;
		this.column = column;
		this.labelProvider = labelProvider;
	}

	public int getColumn() {
		return column;
	}

	public boolean isAscending() {
		return ascending;
	}

	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	/**
	 * Returns the comparison of the two objects in ascending mode. If the
	 * viewer is set to descending, we invert the result of this method. This
	 * function should be overwritten by sub-classes if instances without a
	 * label provider are created.
	 */
	public int compare(T obj1, T obj2) {
		if (labelProvider == null)
			return 0;
		String text1 = labelProvider.getColumnText(obj1, column);
		String text2 = labelProvider.getColumnText(obj2, column);
		return Strings.compare(text1, text2);
	}

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
