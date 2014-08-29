package org.openlca.app.viewers.table.modify;

@FunctionalInterface
public interface Getter<T> {

	String getText(T element);

}
