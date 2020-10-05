package org.openlca.app.viewers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

public class Selections {

	private Selections() {
	}

	public static <T> T firstOf(SelectionChangedEvent e) {
		return e == null
				? null
				: firstOf(e.getSelection());
	}

	public static <T> T firstOf(DoubleClickEvent e) {
		return e == null
				? null
				: firstOf(e.getSelection());
	}

	public static <T> List<T> allOf(SelectionChangedEvent e) {
		return e == null
				? Collections.emptyList()
				: allOf(e.getSelection());
	}

	/**
	 * Try to get the first element from the given selection. Returns null if the
	 * selection is empty. Note that the caller has to make sure that the selection
	 * contains the specified type.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T firstOf(ISelection s) {
		if (isEmpty(s))
			return null;
		var struct = (IStructuredSelection) s;
		return (T) struct.getFirstElement();
	}

	/**
	 * Try to get all elements from the given selection. Returns an empty collection
	 * if the selection is empty. Note that the caller has to make sure that the
	 * selection contains the specified type.
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> allOf(ISelection s) {
		if (isEmpty(s))
			return Collections.emptyList();
		var struct = (IStructuredSelection) s;
		var list = new ArrayList<T>();
		for (Object o : struct) {
			list.add((T) o);
		}
		return list;
	}

	/**
	 * Returns true if the given selection is an instance of `IStructuredSelection`
	 * that is not empty.
	 */
	public static boolean isEmpty(ISelection s) {
		return s == null
				|| s.isEmpty()
				|| !(s instanceof IStructuredSelection);
	}
}
