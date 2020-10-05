package org.openlca.app.viewers.tables.modify;

@FunctionalInterface
public interface Getter<T> {

	String getText(T element);

}
