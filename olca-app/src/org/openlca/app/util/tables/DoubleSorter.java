package org.openlca.app.util.tables;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DoubleSorter<T> extends Sorter<T> {

	private final Function<T, Double> fn;

	DoubleSorter(int column, Function<T, Double> fn) {
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
			Double d1 = fn.apply((T) e1);
			Double d2 = fn.apply((T) e2);
			if (d1 == null && d2 == null)
				return 0;
			if (d1 == null || d2 == null)
				return d1 == null ? -1 : 1;
			return d1.compareTo(d2);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to compare " + e1 + " and " + e2
					+ " with double function on column " + column, e);
			return 0;
		}
	}
}
