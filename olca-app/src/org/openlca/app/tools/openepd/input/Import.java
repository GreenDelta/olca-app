package org.openlca.app.tools.openepd.input;

import org.openlca.app.tools.openepd.model.Ec3Epd;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.io.ImportLog;
import org.openlca.core.model.EpdModule;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowResult;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ImpactResult;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Result;
import org.openlca.util.Strings;

import java.util.HashMap;

class Import {

	private final IDatabase db;
	private final Ec3Epd epd;
	private final ImportMapping mapping;
	private final Quantity quantity;
	private final String category;
	private final ImportLog log;

	Import(IDatabase db, Ec3Epd epd, ImportMapping mapping) {
		this.db = db;
		this.epd = epd;
		this.mapping = mapping;
		this.quantity = mapping.quantity();
		this.category = Util.categoryOf(epd);
		this.log = new ImportLog();
	}

	void run() {

		var refFlow = createRefFlow();

		var modules = new HashMap<String, EpdModule>();
		for (var result : epd.impactResults) {
			var methodMapping = mapping.getMethodMapping(result.method());
			if (methodMapping.isEmpty()) {
				log.warn("No mapping for LCIA method '"
					+ result.method() + "'; results skipped");
				continue;
			}

			for (var indicatorResult : result.indicatorResults()) {
				for (var scopeResult : indicatorResult.values()) {
					if (scopeResult.value() == null)
						continue;

					var val = scopeResult.value();
					var key = new IndicatorKey(indicatorResult.indicator(), val.unit());
					var indicatorMapping = methodMapping.getIndicatorMapping(key);
					if (indicatorMapping.isEmpty()) {
						log.warn("No mapping for LCIA category '"
							+ key.code() + "'; results skipped");
						continue;
					}

					// get/init the module result
					var fullName = refFlow.flow.name
						+ " - " + scopeResult.scope()
						+ " - " + result.method();
					var mod = modules.computeIfAbsent(fullName, _k -> {
						var modResult = initResult(
							fullName, refFlow, methodMapping.method());
						return EpdModule.of(scopeResult.scope(), modResult);
					});

					// add the result value
					var impact = ImpactResult.of(indicatorMapping.indicator(), val.mean());
					mod.result.impactResults.add(impact);
				}
			}
		}

		// persist the module results
		for (var mod : modules.values()) {
			var result = db.insert(mod.result);
			log.imported(result);
			mod.result = result;
		}

	}

	private FlowResult createRefFlow() {
		var product = Flow.product(epd.productName, quantity.property());
		if (Strings.notEmpty(category)) {
			product.category = CategoryDao.sync(db, ModelType.FLOW, category);
		}
		product.description = epd.productDescription;
		product = db.insert(product);
		log.imported(product);
		var refFlow = FlowResult.outputOf(product, quantity.amount());
		if (quantity.hasUnit()) {
			refFlow.unit = quantity.unit();
		}
		return refFlow;
	}

	private Result initResult(String name, FlowResult refFlow,
														ImpactMethod method) {
		var qRef = refFlow.copy();
		var result = Result.of(name);
		result.impactMethod = method;
		result.description = "Imported from openEPD: " + epd.id;
		if (Strings.notEmpty(category)){
			result.category = CategoryDao.sync(db, ModelType.RESULT, category);
		}
		result.referenceFlow = qRef;
		result.flowResults.add(qRef);
		return result;
	}
}
