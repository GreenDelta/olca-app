package org.openlca.app.viewers;

@FunctionalInterface
public interface ISelectionChangedListener<T> {

	public void selectionChanged(T selection);

}
