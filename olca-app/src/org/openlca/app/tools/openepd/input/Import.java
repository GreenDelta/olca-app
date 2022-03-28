package org.openlca.app.tools.openepd.input;

import java.util.Collection;
import java.util.HashMap;

import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.io.ImportLog;
import org.openlca.core.model.Actor;
import org.openlca.core.model.Category;
import org.openlca.core.model.Epd;
import org.openlca.core.model.EpdModule;
import org.openlca.core.model.EpdProduct;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowResult;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ImpactResult;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Result;
import org.openlca.core.model.Source;
import org.openlca.core.model.Version;
import org.openlca.io.openepd.EpdDoc;
import org.openlca.io.openepd.EpdOrg;
import org.openlca.io.openepd.EpdPcr;
import org.openlca.util.KeyGen;
import org.openlca.util.Strings;

class Import {

	private final IDatabase db;
	private final EpdDoc epd;
	private final ImportMapping mapping;
	private final Quantity quantity;
	private final String[] categoryPath;
	private final ImportLog log;

	Import(IDatabase db, EpdDoc epd, ImportMapping mapping) {
		this.db = db;
		this.epd = epd;
		this.mapping = mapping;
		this.quantity = mapping.quantity();
		this.categoryPath = Util.categoryOf(epd).orElse(null);
		this.log = new ImportLog();
	}

	public ImportLog log() {
		return log;
	}

	Epd run() {

		var refFlow = createRefFlow();
		var modules = createModules(refFlow);

		var e = new Epd();
		e.name = epd.productName;
		e.refId = epd.id;
		e.description = epd.lcaDiscussion;
		e.modules.addAll(modules);
		e.urn = "openEPD:" + epd.id;
		e.category = syncCategory(ModelType.EPD);
		e.lastChange = System.currentTimeMillis();
		e.product = EpdProduct.of(refFlow.flow, quantity.amount());
		e.product.unit = quantity.unit();

		e.manufacturer = getActor(epd.manufacturer);
		e.verifier = getActor(epd.verifier);
		e.programOperator = getActor(epd.programOperator);
		e.pcr = getSource(epd.pcr);

		e = db.insert(e);
		log.imported(e);
		return e;
	}

	private Category syncCategory(ModelType type) {
		return categoryPath == null || categoryPath.length == 0
			? null
			: CategoryDao.sync(db, type, categoryPath);
	}

	private FlowResult createRefFlow() {
		var product = Flow.product(epd.productName, quantity.property());
		product.category = syncCategory(ModelType.FLOW);
		product.description = epd.productDescription;
		product = db.insert(product);
		log.imported(product);
		var refFlow = FlowResult.outputOf(product, quantity.amount());
		if (quantity.hasUnit()) {
			refFlow.unit = quantity.unit();
		}
		return refFlow;
	}

	private Actor getActor(EpdOrg org) {
		if (org == null || Strings.nullOrEmpty(org.name))
			return null;
		var id = Strings.notEmpty(org.ref)
			? KeyGen.get(org.ref)
			: KeyGen.get(org.name);
		var actor = db.get(Actor.class, id);
		if (actor != null)
			return actor;
		actor = Actor.of(org.name);
		actor.refId = id;
		actor.website = org.webDomain;
		actor.address = org.ref;
		actor = db.insert(actor);
		log.imported(actor);
		return actor;
	}

	private Source getSource(EpdPcr pcr) {
		if (pcr == null || Strings.nullOrEmpty(pcr.name))
			return null;
		var id = pcr.id;
		if (Strings.nullOrEmpty(id)) {
			id = Strings.notEmpty(pcr.ref)
				? KeyGen.get(pcr.ref)
				: KeyGen.get(pcr.name);
		}
		var source = db.get(Source.class, id);
		if (source != null)
			return source;
		source = Source.of(pcr.name);
		source.refId = id;
		source.url = pcr.ref;
		source.version = Version.fromString(pcr.version).getValue();
		source = db.insert(source);
		log.imported(source);
		return source;
	}

	private Collection<EpdModule> createModules(FlowResult refFlow) {
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
		return modules.values();
	}

	private Result initResult(String name, FlowResult refFlow,
		ImpactMethod method) {
		var qRef = refFlow.copy();
		var result = Result.of(name);
		result.impactMethod = method;
		result.description = "Imported from openEPD: " + epd.id;
		result.category = syncCategory(ModelType.RESULT);
		result.referenceFlow = qRef;
		result.flowResults.add(qRef);
		return result;
	}
}
