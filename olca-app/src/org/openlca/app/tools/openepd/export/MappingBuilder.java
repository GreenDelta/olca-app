package org.openlca.app.tools.openepd.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.openlca.core.model.Epd;
import org.openlca.core.model.EpdModule;
import org.openlca.core.model.ImpactCategory;
import org.openlca.io.openepd.Vocab;
import org.openlca.util.Strings;

class MappingBuilder {

	static List<MappingModel> buildFrom(Epd epd) {
		if (epd == null)
			return Collections.emptyList();

		// create models and their rows
		var models = new ArrayList<MappingModel>();
		for (var mod : epd.modules) {
			var model = of(mod, models);
			if (model == null)
				continue;
			for (var impact : mod.result.impactResults) {
				var indicator = impact.indicator;
				if (indicator == null)
					continue;
				var row = rowOf(indicator, model);
				row.values().put(scopeOf(mod), mod.multiplier * impact.amount);
			}
		}

		// sort & map
		for (var model : models) {
			model.scopes().sort(Strings::compare);
			model.rows().sort((r1, r2) -> {
				var i1 = r1.indicator();
				var i2 = r2.indicator();
				return i1 != null && i2 != null
					? Strings.compare(i1.name, i2.name)
					: 0;
			});
			initMappings(model);
		}
		return models;
	}

	private static MappingModel of(EpdModule mod, List<MappingModel> models) {
		if (mod == null
			|| Strings.nullOrEmpty(mod.name)
			|| mod.result == null
			|| mod.result.impactResults.isEmpty())
			return null;
		var method = mod.result.impactMethod;
		MappingModel model = null;
		for (var existing : models) {
			if (Objects.equals(method, existing.method())) {
				model = existing;
				break;
			}
		}
		if (model == null) {
			model = new MappingModel().method(method);
			models.add(model);
		}
		var scope = scopeOf(mod);
		if (!model.scopes().contains(scope)) {
			model.scopes().add(scope);
		}
		return model;
	}

	private static String scopeOf(EpdModule mod) {
		if (mod == null || mod.name == null)
			return "?";
		var scope = Vocab.findScope(mod.name);
		return scope.isPresent()
			? scope.get().code()
			: mod.name;
	}

	private static MappingRow rowOf(ImpactCategory indicator, MappingModel model) {
		for (var row : model.rows()) {
			if (Objects.equals(indicator, row.indicator()))
				return row;
		}
		var row = new MappingRow().indicator(indicator);
		model.rows().add(row);
		return row;
	}

	private static void initMappings(MappingModel model) {

		var queue = EnumSet.allOf(Vocab.Indicator.class);
		Supplier<Vocab.Indicator> next = () -> {
			var i = queue.iterator().next();
			queue.remove(i);
			return i;
		};

		while (!queue.isEmpty()) {
			var epdInd = next.get();
			var match = Match.empty();
			for (var row : model.rows()) {
				var nextMatch = Match.of(epdInd, row);
				if (match.isBetterThan(nextMatch))
					continue;
				var prevMatch = Match.of(row);
				if (prevMatch.isBetterThan(nextMatch))
					continue;

				match.release();
				if (prevMatch.isBound()) {
					if (prevMatch.indicator() != epdInd) {
						queue.add(prevMatch.indicator());
					}
					prevMatch.release();
				}
				nextMatch.bind();
				match = nextMatch;
			}
		}
	}

	private record Match(
		Vocab.Indicator indicator,
		MappingRow row,
		Vocab.UnitMatch unit,
		double score) {

		private static final Match _empty = new Match(null, null, null, 0);

		static Match empty() {
			return _empty;
		}

		static Match of(MappingRow row) {
			return of(row.epdIndicator(), row);
		}

		static Match of(Vocab.Indicator indicator, MappingRow row) {
			if (indicator == null || row == null || row.indicator() == null)
				return empty();
			var refUnit = row.indicator().referenceUnit;
			var unitMatch = indicator.unitMatchOf(refUnit)
				.orElse(null);
			if (unitMatch == null)
				return empty();
			var score = Objects.equals(indicator.code(), row.indicator().code)
				? 1.0
				: indicator.matchScoreOf(row.indicator().name);
			if (score < 1e-4)
				return empty();
			return new Match(indicator, row, unitMatch, score);
		}

		boolean isEmpty() {
			return indicator == null
				|| row == null
				|| unit == null;
		}

		boolean isBetterThan(Match other) {
			if (isEmpty())
				return false;
			return other.isEmpty() || score > other.score;
		}

		void bind() {
			if (isEmpty())
				return;
			row.epdIndicator(indicator)
				.unit(unit)
				.factor(unit.factor());
		}

		boolean isBound() {
			return !isEmpty() && row.epdIndicator() == indicator;
		}

		void release() {
			if (!isBound())
				return;
			row.epdIndicator(null)
				.unit(null)
				.factor(1);
		}

	}
}
