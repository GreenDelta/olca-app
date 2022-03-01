package org.openlca.app.tools.openepd.input;

import org.openlca.app.tools.openepd.model.Ec3Epd;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

record ImportMapping(
	Quantity quantity,
	Map<String, MethodMapping> methodMappings) {

	static ImportMapping init(Ec3Epd epd, IDatabase db) {

		// collect the method codes and related indicator keys
		var codes = new HashMap<String, Set<IndicatorKey>>();
		for (var result : epd.impactResults) {
			var code = result.method();
			var keys = codes.computeIfAbsent(code, _code -> new HashSet<>());
			for (var i : result.indicatorResults()) {
				var unit = i.values().stream()
					.filter(v -> v.value() != null)
					.map(v -> v.value().unit())
					.filter(Objects::nonNull)
					.findAny()
					.orElse("");
				keys.add(new IndicatorKey(i.indicator(), unit));
			}
		}

		// initialize the method mappings
		var methods = db.allOf(ImpactMethod.class);
		var mappings = new HashMap<String, MethodMapping>();
		for (var e : codes.entrySet()) {
			var code = e.getKey();
			var keys = e.getValue();
			ImpactMethod method = null;
			for (var m : methods) {
				if (sameCode(code, m.code)) {
					method = m;
					break;
				}
			}
			var mapping = method != null
				? MethodMapping.init(code, method, keys)
				: MethodMapping.emptyOf(code, keys);
			mappings.put(code, mapping);
		}

		var quantity = Quantity.detect(epd, db);
		return new ImportMapping(quantity, mappings);
	}

	static boolean sameCode(String code1, String code2) {
		if (Strings.nullOrEmpty(code1) || Strings.nullOrEmpty(code2))
			return false;
		return code1.trim().equalsIgnoreCase(code2.trim());
	}

	MethodMapping getMethodMapping(String code) {
		var mapping = methodMappings.get(code);
		if (mapping != null)
			return mapping;
		var empty = MethodMapping.emptyOf(code, Collections.emptySet());
		methodMappings.put(code, empty);
		return empty;
	}

	List<String> methodCodes() {
		return methodMappings.keySet()
			.stream()
			.sorted()
			.toList();
	}
}

record MethodMapping(
	String code,
	ImpactMethod method,
	List<IndicatorMapping> indicatorMappings) {

	static MethodMapping emptyOf(String code, Set<IndicatorKey> keys) {
		var indicatorMappings = keys.stream()
			.map(IndicatorMapping::emptyOf)
			.toList();
		return new MethodMapping(code, null, indicatorMappings);
	}

	static MethodMapping init(
		String code, ImpactMethod method, Set<IndicatorKey> keys) {
		var mappings = new ArrayList<IndicatorMapping>();
		for (var key : keys) {
			ImpactCategory impact = null;
			for (var i : method.impactCategories) {
				if (ImportMapping.sameCode(key.code(), i.code)) {
					impact = i;
					break;
				}
			}
			mappings.add(new IndicatorMapping(key, impact));
		}
		return new MethodMapping(code, method, mappings);
	}

	boolean isEmpty() {
		return method == null;
	}

	List<IndicatorKey> keys() {
		return indicatorMappings.stream()
			.map(IndicatorMapping::key)
			.sorted()
			.toList();
	}
}

record IndicatorMapping(IndicatorKey key, ImpactCategory indicator) {

	static IndicatorMapping emptyOf(IndicatorKey key) {
		return new IndicatorMapping(key, null);
	}

	boolean isEmpty() {
		return indicator == null;
	}

	String code() {
		return key.code();
	}

	String unit() {
		return key.unit();
	}
}

record IndicatorKey(String code, String unit)
	implements Comparable<IndicatorKey> {

	@Override
	public int compareTo(IndicatorKey other) {
		return other != null
			? Strings.compare(this.code, other.code)
			: 1;
	}
}
