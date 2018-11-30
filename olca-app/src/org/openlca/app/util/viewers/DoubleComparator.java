package org.openlca.app.util.viewers;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DoubleComparator<T> extends Comparator<T> {

	private final Function<T, Double> fn;

	DoubleComparator(int column, Function<T, Double> fn) {
		super(column);
		this.fn = fn;
	}

	@Override
	protected int compare(T e1, T e2) {
		try {
			Double d1 = fn.apply(e1);
			Double d2 = fn.apply(e2);
			if (d1 == null && d2 == null)
				return 0;
			if (d1 == null || d2 == null)
				return d1 == null ? -1 : 1;
			return d1.compareTo(d2);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to compare " + e1 + " and " + e2 + " with double function on column " + column, e);
			return 0;
		}
	}
}
