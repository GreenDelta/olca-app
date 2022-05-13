package org.openlca.app.tools.openepd.export;

import org.openlca.core.model.Epd;
import org.openlca.core.model.EpdModule;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.io.openepd.Vocab;
import org.openlca.io.openepd.Vocab.Indicator;
import org.openlca.io.openepd.Vocab.UnitMatch;
import org.openlca.util.Strings;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

class ResultModel {
	// The openLCA LCIA method; can be `null`.
	final ImpactMethod method;

	// The assigned openEPD method.
	Vocab.Method epdMethod;

	final List<String> scopes = new ArrayList<>();
	final List<ResultRow> rows = new ArrayList<>();

	ResultModel(ImpactMethod method) {
		this.method = method;
	}

	static List<ResultModel> allOf(Epd epd) {

		// create models and their rows
		var models = new ArrayList<ResultModel>();
		for (var mod : epd.modules) {
			var model = of(mod, models);
			if (model == null)
				continue;
			for (var impact : mod.result.impactResults) {
				var indicator = impact.indicator;
				if (indicator == null)
					continue;
				var row = rowOf(indicator, model);
				row.values.put(mod.name, mod.multiplier * impact.amount);
			}
		}

		// sort & map
		for (var model : models) {
			model.scopes.sort(Strings::compare);
			model.rows.sort((r1, r2) -> Strings.compare(
				r1.indicator.name, r2.indicator.name));
			model.initMappings();
		}

		return models;
	}

	private static ResultModel of(EpdModule mod, List<ResultModel> models) {
		if (mod == null
			|| Strings.nullOrEmpty(mod.name)
			|| mod.result == null
			|| mod.result.impactResults.isEmpty())
			return null;
		var method = mod.result.impactMethod;
		ResultModel model = null;
		for (var existing : models) {
			if (Objects.equals(method, existing.method)) {
				model = existing;
				break;
			}
		}
		if (model == null) {
			model = new ResultModel(method);
			models.add(model);
		}
		if (!model.scopes.contains(mod.name)) {
			model.scopes.add(mod.name);
		}
		return model;
	}

	private static ResultRow rowOf(ImpactCategory indicator, ResultModel model) {
		for (var row : model.rows) {
			if (Objects.equals(indicator, row.indicator))
				return row;
		}
		var row = new ResultRow(indicator);
		model.rows.add(row);
		return row;
	}

	private void initMappings() {

		var queue = EnumSet.allOf(Vocab.Indicator.class);
		Supplier<Indicator> next = () -> {
			var i = queue.iterator().next();
			queue.remove(i);
			return i;
		};

		while (!queue.isEmpty()) {
			var epdInd = next.get();
			var match = Match.empty();
			for (var row : rows) {
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
		ResultRow row,
		UnitMatch unitMatch,
		double score) {

		private static final Match _empty = new Match(null, null, null, 0);

		static Match empty() {
			return _empty;
		}

		static Match of(ResultRow row) {
			return of(row.epdIndicator, row);
		}

		static Match of(Vocab.Indicator indicator, ResultRow row) {
			if (indicator == null || row == null)
				return empty();
			var refUnit = row.indicator.referenceUnit;
			var unitMatch = indicator.unitMatchOf(refUnit)
				.orElse(null);
			if (unitMatch == null)
				return empty();
			var score = indicator.matchScoreOf(row.indicator.name);
			if (score < 1e-4)
				return empty();
			return new Match(indicator, row, unitMatch, score);
		}

		boolean isEmpty() {
			return indicator == null
				|| row == null
				|| unitMatch == null;
		}

		boolean isBetterThan(Match other) {
			if (isEmpty())
				return false;
			return other.isEmpty() || score > other.score;
		}

		void bind() {
			if (isEmpty())
				return;
			row.epdIndicator = indicator;
			row.unitMatch = unitMatch;
			row.factor = unitMatch.factor();
		}

		boolean isBound() {
			return !isEmpty() && row.epdIndicator == indicator;
		}

		void release() {
			if (!isBound())
				return;
			row.epdIndicator = null;
			row.unitMatch = null;
			row.factor = 1;
		}

	}
}
