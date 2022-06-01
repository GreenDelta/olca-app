package org.openlca.app.tools.openepd.input;

import java.util.HashSet;

import org.openlca.app.util.Question;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ModelType;
import org.openlca.io.openepd.EpdDoc;
import org.openlca.io.openepd.io.MappingModel;

record ImpactMappings(IDatabase db, EpdDoc doc) {

	static MappingModel askCreate(IDatabase db, EpdDoc doc) {
		return new ImpactMappings(db, doc).runCheck();
	}

	private MappingModel runCheck() {

		// create and check default mapping
		var mapping = MappingModel.initFrom(doc, db);
		var unmapped = new HashSet<String>();
		for (var m : mapping.mappings()) {
			if (m.epdMethod() == null)
				continue;
			if (m.method() == null) {
				unmapped.add(m.epdMethod().code());
			}
		}
		if (unmapped.isEmpty())
			return mapping;

		// ask to create
		var q = "The EPD data set contains one or more unknown LCIA methods: "
			+ String.join(", ", unmapped) + ". Do you want to create default"
			+ "LCIA methods and indicators?";
		if (!Question.ask("Create default LCIA method(s)?", q))
			return mapping;

		var methodCategory = CategoryDao.sync(
			db, ModelType.IMPACT_METHOD, "openEPD");
		for (var m : mapping.mappings()) {
			if (m.epdMethod() == null || m.method() != null)
				continue;
			var methodCode = m.epdMethod().code();
			var method = ImpactMethod.of(methodCode);
			method.code = methodCode;
			method.category = methodCategory;
			var indicatorCategory = CategoryDao.sync(
				db, ModelType.IMPACT_CATEGORY, "openEPD", methodCode);
			for (var entry : m.entries()) {
				var epdInd = entry.epdIndicator();
				if (epdInd == null)
					continue;
				var impact = ImpactCategory.of(
					epdInd.code().toUpperCase(),
					epdInd.unit());
				impact.description = epdInd.description();
				impact.code = epdInd.code();
				impact.category = indicatorCategory;
				impact = db.insert(impact);
				method.impactCategories.add(impact);
				entry.indicator(impact);
				entry.unit(epdInd.unitMatchOf(epdInd.unit()).orElse(null));
			}
			m.method(db.insert(method));
		}

		return mapping;
	}

}
