package org.openlca.app.tools.openepd;

import java.util.HashSet;

import org.openlca.app.util.Question;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ModelType;
import org.openlca.io.openepd.EpdDoc;
import org.openlca.io.openepd.input.ImpactMapping;

record ImpactMappings(IDatabase db, EpdDoc doc) {

	static ImpactMapping askCreate(IDatabase db, EpdDoc doc) {
		return new ImpactMappings(db, doc).runCheck();
	}

	private ImpactMapping runCheck() {

		// create and check default mapping
		var mapping = ImpactMapping.init(doc, db);
		var candidates = new HashSet<String>();
		for (var e : mapping.map().entrySet()) {
			var m = e.getValue();
			if (m.isEmpty()) {
				candidates.add(e.getKey());
			}
		}
		if (candidates.isEmpty())
			return mapping;

		// ask to create
		var q = "The EPD data set contains one or more unknown LCIA methods: "
			+ String.join(", ", candidates) + ". Do you want to create default"
			+ "LCIA methods and indicators?";
		if (!Question.ask("Create default LCIA method(s)?", q))
			return mapping;

		var methodCategory = CategoryDao.sync(
			db, ModelType.IMPACT_METHOD, "openEPD");
		for (var code : candidates) {
			var m = mapping.getMethodMapping(code);
			var method = ImpactMethod.of(code);
			method.code = code;
			method.category = methodCategory;
			var indicatorCategory = CategoryDao.sync(
				db, ModelType.IMPACT_CATEGORY, "openEPD", code);
			for (var im : m.indicatorMappings()) {
				if (im.code() == null)
					continue;
				var impact = ImpactCategory.of(
					code + " - " + im.code().toUpperCase(), im.unit());
				impact.code = im.code();
				impact.category = indicatorCategory;
				impact = db.insert(impact);
				method.impactCategories.add(impact);
			}
			db.insert(method);
		}

		return ImpactMapping.init(doc, db);
	}

}
