package org.openlca.app.viewers.table.modify;

@FunctionalInterface
public interface Setter<T> {

	void setText(T element, String text);

}
