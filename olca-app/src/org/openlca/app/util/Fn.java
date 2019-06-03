package org.openlca.app.util;

import java.util.function.Consumer;

public final class Fn {

	private Fn() {
	}

	public static <T> void with(T t, Consumer<T> fn) {
		if (t != null && fn != null) {
			fn.accept(t);
		}
	}

}
