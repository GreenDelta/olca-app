package org.openlca.app.util;

import java.util.HashSet;
import java.util.Locale;
import java.util.function.Function;

public class Names {

	public static <T> String uniqueOf(
		String base, Iterable<T> existing, Function<T, String> fn) {
		var raw = base == null ? "" : base.trim();
		if (existing == null)
			return raw;
		var s = new HashSet<String>();
		for (var e : existing) {
			var en = fn.apply(e);
			if (en != null) {
				s.add(en.trim().toLowerCase(Locale.US));
			}
		}

		var norm = raw.trim().toLowerCase(Locale.US);
		if (!s.contains(norm))
			return raw;

		int i = 1;
		String nextNorm;
		do {
			i++;
			nextNorm = norm + " (" + i + ")";
		} while (s.contains(nextNorm));
		return raw.trim() + " (" + i + ")";
	}

}
