package org.openlca.app.util.viewers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.trees.Trees;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Viewers {

	private final static Logger log = LoggerFactory.getLogger(Viewers.class);

	/** Get the first selected element from the given viewer. */
	public static <T> T getFirstSelected(StructuredViewer viewer) {
		if (viewer == null)
			return null;
		ISelection selection = viewer.getSelection();
		return getFirst(selection);
	}

	/** Get the first element from the given selection. */
	public static <T> T getFirst(ISelection selection) {
		if (!(selection instanceof IStructuredSelection) || selection.isEmpty())
			return null;
		IStructuredSelection structSelection = (IStructuredSelection) selection;
		try {
			// caller has to assign the right class
			@SuppressWarnings("unchecked")
			T obj = (T) structSelection.getFirstElement();
			return obj;
		} catch (ClassCastException e) {
			log.error("Error casting obj of type " + structSelection.getFirstElement().getClass().getCanonicalName(),
					e);
			return null;
		}
	}

	/** Get all selected elements from the given viewer. */
	public static <T> List<T> getAllSelected(StructuredViewer viewer) {
		if (viewer == null)
			return Collections.emptyList();
		IStructuredSelection s = (IStructuredSelection) viewer.getSelection();
		return getAll(s);
	}

	/** Get all elements from the given selection. */
	public static <T> List<T> getAll(IStructuredSelection selection) {
		if (selection == null || selection.isEmpty())
			return Collections.emptyList();
		List<T> list = new ArrayList<>();
		for (Object o : selection) {
			try {
				// caller has to assign to right class
				@SuppressWarnings("unchecked")
				T obj = (T) o;
				list.add(obj);
			} catch (ClassCastException e) {
				log.error("Error casting obj of type "
						+ o.getClass().getCanonicalName(), e);
			}
		}
		return list;
	}

	public static <T> void sortByDouble(
			ColumnViewer viewer, Function<T, Double> fn, int col) {
 		addComparator(viewer, Comparator.on(col, (T e1, T e2) -> {
			double d1 = fn.apply(e1);
			double d2 = fn.apply(e2);
			return Double.compare(d1, d2);
		}));
	}

	public static <T> void sortByDouble(
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
