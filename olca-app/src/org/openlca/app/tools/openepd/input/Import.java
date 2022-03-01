package org.openlca.app.tools.openepd.input;

import org.openlca.app.tools.openepd.model.Ec3Epd;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.EpdModule;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowResult;
import org.openlca.core.model.Result;

import java.util.HashMap;

class Import {

	private final IDatabase db;
	private final Ec3Epd epd;
	private final ImportMapping mapping;
	private final Quantity quantity;

	Import(IDatabase db, Ec3Epd epd, ImportMapping mapping) {
		this.db = db;
		this.epd = epd;
		this.mapping = mapping;
		this.quantity = mapping.quantity();
	}

	void run() {

		// create the declared product and the reference flow of the results
		var product = Flow.product(epd.productName, quantity.property());
		// TODO category
		product.description = epd.productDescription;
		product = db.insert(product);
		var refFlow = FlowResult.outputOf(product, quantity.amount());
		if (quantity.hasUnit()) {
			refFlow.unit = quantity.unit();
		}

		var modules = new HashMap<String, EpdModule>();
		for (var result : epd.impactResults) {
			for (var i : result.indicatorResults()) {
				for (var val : i.values()) {
					var fullName = product.name
						+ " - " + val.scope()
						+ " - " + result.method();
					var mod = modules.computeIfAbsent(fullName, _k -> {
						var modResult = resultOf(fullName, refFlow);
						return EpdModule.of(val.scope(), modResult);
					});

				}
			}
		}

	}

	private Result resultOf(String name, FlowResult refFlow) {
		var qRef = refFlow.copy();
		var result = Result.of(name);
		result.description = "Imported from openEPD: " + epd.id;
		// TODO category
		result.referenceFlow = qRef;
		result.flowResults.add(qRef);
		return db.insert(result);
	}
}
