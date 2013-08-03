package org.openlca.app.viewers.table.modify;

public interface IModelChangedListener<T> {

	public enum ModelChangeType {

		CREATE, REMOVE, CHANGE;

	}

	void modelChanged(IModelChangedListener.ModelChangeType type, T element);

}