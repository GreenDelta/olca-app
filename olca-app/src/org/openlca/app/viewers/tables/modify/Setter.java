package org.openlca.app.viewers.tables.modify;

@FunctionalInterface
public interface Setter<T> {

	void setText(T element, String text);

}
