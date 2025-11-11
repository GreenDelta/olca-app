package org.openlca.app.results.analysis.groups;

import java.util.List;
import java.util.Optional;

import org.openlca.app.db.Database;
import org.openlca.commons.Strings;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.AnalysisGroup;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.Category;
import org.openlca.core.model.Epd;
import org.openlca.core.model.EpdModule;
import org.openlca.core.model.FlowResult;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ImpactResult;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Result;

class EpdBuilder {

	private final IDatabase db;
	private final ProductSystem system;
	private final ImpactMethod method;
	private final List<ImpactGroupResult> results;
	private final String name;
	private final List<String> categoryPath;

	EpdBuilder(
			IDatabase db,
			ProductSystem system,
			ImpactMethod method,
			List<ImpactGroupResult> results,
			String name,
			List<String> categoryPath) {
		this.db = db;
		this.system = system;
		this.method = method;
		this.results = results;
		this.name = name;
		this.categoryPath = categoryPath;
	}

	static Optional<Epd> build(
			CalculationSetup setup,
			List<ImpactGroupResult> results,
			String name,
			List<String> category
	) {
		var db = Database.get();
		if (db == null)
			return Optional.empty();
		if (setup == null
				|| !(setup.target() instanceof ProductSystem system))
			return Optional.empty();

		if (setup.impactMethod() == null
				|| system.analysisGroups.isEmpty()
				|| system.referenceExchange == null
				|| system.referenceExchange.flow == null
				|| results.isEmpty()
				|| Strings.isBlank(name))
			return Optional.empty();
		return new EpdBuilder(
				db, system, setup.impactMethod(), results, name, category
		).build();
	}

	private Optional<Epd> build() {

		var qRef = system.referenceExchange;
		var epd = Epd.of(name, qRef.flow);
		epd.product.amount = system.targetAmount;
		epd.product.unit = system.targetUnit != null
				? system.targetUnit
				: qRef.unit;
		var prop = system.targetFlowPropertyFactor != null
				? system.targetFlowPropertyFactor
				: qRef.flowPropertyFactor;
		if (prop != null) {
			epd.product.property = prop.flowProperty;
		}

		var resultCategory = categoryOf(ModelType.RESULT);
		for (var group : system.analysisGroups) {
			var result = resultOf(group, resultCategory);
			if (result == null)
				continue;
			var mod = EpdModule.of(group.name, result);
			epd.modules.add(mod);
		}

		if (epd.modules.isEmpty())
			return Optional.empty();
		epd.category = categoryOf(ModelType.EPD);
		return Optional.of(db.insert(epd));
	}

	private Result resultOf(AnalysisGroup group, Category category) {
		if (group == null || Strings.isBlank(group.name))
			return null;

		var r = Result.of(name + " - " + group.name);
		r.category = category;
		var refFlow = makeRefFlow();
		r.flowResults.add(refFlow);
		r.referenceFlow = refFlow;

		r.impactMethod = method;
		for (var impact : method.impactCategories) {
			var i = findResult(impact);
			if (i == null)
				continue;
			var value = i.values().get(group.name);
			if (value == null)
				continue;
			var ir = ImpactResult.of(impact, value);
			r.impactResults.add(ir);
		}

		return db.insert(r);
	}

	private ImpactGroupResult findResult(ImpactCategory impact) {
		if (impact == null)
			return null;
		for (var i : results) {
			if (i.impact() == null || i.values() == null)
				return null;
			if (i.impact().id == impact.id)
				return i;
		}
		return null;
	}

	private FlowResult makeRefFlow() {
		var e = system.referenceExchange;
		var r = e.isInput
				? FlowResult.inputOf(e.flow, system.targetAmount)
				: FlowResult.outputOf(e.flow, system.targetAmount);
		r.flowPropertyFactor = system.targetFlowPropertyFactor != null
				? system.targetFlowPropertyFactor
				: e.flowPropertyFactor;
		r.unit = system.targetUnit != null
				? system.targetUnit
				: e.unit;
		return r;
	}

	private Category categoryOf(ModelType type) {
		if (categoryPath == null || categoryPath.isEmpty() || type == null)
			return null;
		var path = categoryPath.stream()
			.filter(Strings::isNotBlank)
			.toArray(String[]::new);
		return path.length > 0
			? CategoryDao.sync(db, type, path)
			: null;
	}
}
