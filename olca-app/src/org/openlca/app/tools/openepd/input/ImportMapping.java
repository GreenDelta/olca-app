package org.openlca.app.tools.openepd.input;

import gnu.trove.map.hash.TLongObjectHashMap;
import org.openlca.app.tools.openepd.model.Ec3Epd;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.util.Strings;

import java.util.ArrayList;
import java.util.Collection;
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

	IndicatorMapping getIndicatorMapping(String methodCode, IndicatorKey key) {
		var methodMapping = getMethodMapping(methodCode);
		for (var i : methodMapping.indicatorMappings()) {
			if (Objects.equals(i.key(), key)) {
				return i;
			}
		}
		return IndicatorMapping.emptyOf(key);
	}

	List<String> methodCodes() {
		return methodMappings.keySet()
			.stream()
			.sorted()
			.toList();
	}

	MethodMapping swapMethod(String code, ImpactMethod method) {
		var current = methodMappings.get(code);
		List<IndicatorKey> keys = current != null
			? current.keys()
			: Collections.emptyList();
		var next = method == null
			? MethodMapping.emptyOf(code, keys)
			: MethodMapping.init(code, method, keys);
		methodMappings.put(code, next);
		return next;
	}

	void swapIndicator(
		String methodCode, IndicatorKey key, ImpactCategory impact) {
		var methodMapping = getMethodMapping(methodCode);
		if (methodMapping.isEmpty())
			return;
		var mappings = methodMapping.indicatorMappings();
		mappings.stream()
			.filter(m -> Objects.equals(m.key(), key))
			.findAny()
			.ifPresent(mappings::remove);
		mappings.add(new IndicatorMapping(key, impact));
	}

	void persistIn(IDatabase db) {
		var persisted = new HashMap<String, MethodMapping>();
		var updatedMethods = new TLongObjectHashMap<ImpactMethod>();
		var updatedIndicators = new TLongObjectHashMap<ImpactCategory>();

		for (var e : methodMappings.entrySet()) {
			var code = e.getKey();
			var mapping = e.getValue();
			if (mapping.isEmpty()) {
				persisted.put(code, mapping);
				continue;
			}

			// update indicator codes
			var indicatorMappings = new ArrayList<IndicatorMapping>();
			for (var i : mapping.indicatorMappings()) {
				if (i.isEmpty()) {
					indicatorMappings.add(i);
					continue;
				}
				var indicator = i.indicator();

				// if it already was updated, then do it just once and
				// take the updated version
				var updated = updatedIndicators.get(indicator.id);
				if (updated != null) {
					indicatorMappings.add(new IndicatorMapping(i.key(), updated));
					continue;
				}

				indicator.code = i.code();
				indicator = db.update(indicator);
				updatedIndicators.put(indicator.id, indicator);
				indicatorMappings.add(new IndicatorMapping(i.key(), indicator));
			}

			// update method code
			var method = mapping.method();
			var updated = updatedMethods.get(method.id);
			if (updated != null) {
				persisted.put(code, new MethodMapping(code, updated, indicatorMappings));
				continue;
			}
			if (sameCode(code, method.code)) {
				persisted.put(code, new MethodMapping(code, method, indicatorMappings));
				continue;
			}

			method.code = code;
			method = db.update(method);
			updatedMethods.put(method.id, method);
			persisted.put(code, new MethodMapping(code, method, indicatorMappings));
		}

		methodMappings.clear();
		methodMappings.putAll(persisted);
	}

}

record MethodMapping(
	String code,
	ImpactMethod method,
	List<IndicatorMapping> indicatorMappings) {

	static MethodMapping emptyOf(String code, Collection<IndicatorKey> keys) {
		var indicatorMappings = keys.stream()
			.map(IndicatorMapping::emptyOf)
			.toList();
		return new MethodMapping(code, null, indicatorMappings);
	}

	static MethodMapping init(
		String code, ImpactMethod method, Collection<IndicatorKey> keys) {
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
