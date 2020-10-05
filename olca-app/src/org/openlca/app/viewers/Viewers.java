package org.openlca.app.viewers;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.trees.Trees;

public class Viewers {

	/** Get the first selected element from the given viewer. */
	public static <T> T getFirstSelected(StructuredViewer viewer) {
		return viewer == null
				? null
				: Selections.firstOf(viewer.getSelection());
	}

	/** Get all selected elements from the given viewer. */
	public static <T> List<T> getAllSelected(StructuredViewer viewer) {
		return viewer == null
				? Collections.emptyList()
				: Selections.allOf(viewer.getSelection());
	}

	public static <T> void sortByDouble(
			ColumnViewer viewer, Function<T, Double> fn, int col) {
 		addComparator(viewer, Comparator.on(col, (T e1, T e2) -> {
			double d1 = fn.apply(e1);
			double d2 = fn.apply(e2);
			return Double.compare(d1, d2);
		}));
	}

	public static void sortByDouble(
			ColumnViewer viewer, 
			ITableLabelProvider 
			labelProvider, int... cols) {
		for (int col : cols) {
			var s = new LabelComparator<>(col, labelProvider);
			s.asNumbers = true;
			addComparator(viewer, s);
		}
	}

	public static <T> void sortByLabels(ColumnViewer viewer,
			ITableLabelProvider label) {
		if (viewer == null || label == null)
			return;
		Object[] props = viewer.getColumnProperties();
		if (props == null)
			return;
		for (int col = 0; col < props.length; col++) {
			LabelComparator<T> s = new LabelComparator<>(col, label);
			addComparator(viewer, s);
		}
	}

	public static <T> void sortByLabels(ColumnViewer viewer,
			ITableLabelProvider labelProvider, int... cols) {
		for (int col : cols) {
			LabelComparator<T> s = new LabelComparator<>(col, labelProvider);
			addComparator(viewer, s);
		}
	}

	private static void addComparator(ColumnViewer viewer, Comparator<?> comparator) {
		if (viewer instanceof TableViewer)
			Tables.addComparator((TableViewer) viewer, comparator);
		else if (viewer instanceof TreeViewer)
			Trees.addComparator((TreeViewer) viewer, comparator);
	}
}
