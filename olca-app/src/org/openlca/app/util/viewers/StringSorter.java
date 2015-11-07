package org.openlca.app.util.viewers;

import java.util.function.Function;

import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StringSorter<T> extends Sorter<T> {

	private final Function<T, String> fn;

	StringSorter(int column, Function<T, String> fn) {
		super(column);
		this.fn = fn;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected int compare(Object e1, Object e2) {
		if (e1 == null && e2 == null)
			return 0;
		if (e1 == null || e2 == null)
			return e1 == null ? -1 : 1;
		try {
			String s1 = fn.apply((T) e1);
			String s2 = fn.apply((T) e2);
			return Strings.compare(s1, s2);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to compare " + e1 + " and " + e2
					+ " with string function on column " + column, e);
			return 0;
		}
	}
}
