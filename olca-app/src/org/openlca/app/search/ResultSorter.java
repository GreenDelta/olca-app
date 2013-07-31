package org.openlca.app.search;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

/**
 * A sorter for search results.
 * 
 * @author Michael Srocka
 * 
 */
class ResultSorter extends ViewerSorter {

	private int column = 0;
	private boolean ascending = true;

	public void sortColumn(int col) {
		if (col == column) {
			ascending = !ascending;
		} else {
			column = col;
			ascending = true;
		}
	}

	public boolean isAscending() {
		return ascending;
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		SearchResult result1 = (SearchResult) e1;
		SearchResult result2 = (SearchResult) e2;
		if (ascending)
			return doCompare(result1, result2);
		return doCompare(result2, result1);
	}

	private int doCompare(SearchResult result1, SearchResult result2) {
		switch (column) {
		case Column.NAME:
			return compare(result1.getModelComponent().getName(), result2
					.getModelComponent().getName());
		case Column.CATEGORY:
			return compare(result1.getCategoryPath(), result2.getCategoryPath());
		case Column.DESCRIPTION:
			return compare(result1.getModelComponent().getDescription(),
					result2.getModelComponent().getDescription());
		case Column.DATABASE:
			return compare(result1.getDatabase().getName(), result2
					.getDatabase().getName());
		default:
			return 0;
		}
	}

	private int compare(String string1, String string2) {
		String s1 = string1 == null ? "" : string1;
		String s2 = string2 == null ? "" : string2;
		return s1.compareToIgnoreCase(s2);
	}

}
