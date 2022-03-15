package org.openlca.app.editors.epds;

import org.openlca.app.tools.openepd.model.Ec3ImpactResult;
import org.openlca.app.tools.openepd.model.Ec3IndicatorResult;
import org.openlca.app.tools.openepd.model.Ec3Epd;
import org.openlca.app.tools.openepd.model.Ec3Measurement;
import org.openlca.app.tools.openepd.model.Ec3ScopeValue;
import org.openlca.core.model.Epd;
import org.openlca.util.Strings;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

class EpdConverter {

	static Validation validate(Epd epd) {
		if (epd == null)
			return Validation.error("The EPD is empty.");
		if (epd.product == null || epd.product.flow == null)
			return Validation.error("The EPD has no product.");
		if (epd.product.unit == null)
			return Validation.error("The EPD has no declared unit");
		if (epd.product.amount == 0)
			return Validation.error("The product amount is 0.");

		for (var module : epd.modules) {
			var result = module.result;
			if (result == null)
				return Validation.error("The EPD contains modules without results.");
			if (result.impactMethod == null)
				return Validation.error(
					"The EPD contains module results without links to LCIA methods.");
			if (Strings.nullOrEmpty(result.impactMethod.code))
				return Validation.error(
					"The EPD contains links to LCIA methods without mapping codes.");
			for (var impact : result.impactResults) {
				if (impact.indicator == null
					|| Strings.nullOrEmpty(impact.indicator.code))
					return Validation.error(
						"The EPD contains links to LCIA categories without mapping codes.");
			}
		}
		return Validation.ok();
	}

	static Ec3Epd convert(Epd epd) {
		var doc = new Ec3Epd();
		doc.isDraft = true;
		doc.isPrivate = true;
		doc.name = epd.name;
		if (epd.product != null && epd.product.unit != null) {
			doc.declaredUnit = epd.product.amount + " " + epd.product.unit.name;
		}
		doc.description = epd.description;
		var today = LocalDate.now();
		doc.dateOfIssue = today;
		doc.dateValidityEnds = LocalDate.of(
			today.getYear() + 1, today.getMonth(), today.getDayOfMonth());

		var docResults = new HashMap<String, Ec3ImpactResult>();
		for (var mod : epd.modules) {
			var result = mod.result;
			if (result == null
				|| result.impactMethod == null
				|| Strings.nullOrEmpty(result.impactMethod.code))
				continue;
			var docResult = docResults.computeIfAbsent(
				result.impactMethod.code,
				code -> new Ec3ImpactResult(code, new ArrayList<>()));

			for (var impact : result.impactResults) {
				if (impact.indicator == null
					|| Strings.nullOrEmpty(impact.indicator.code))
					continue;
				var code = impact.indicator.code;
				Ec3IndicatorResult docImpact = null;
				for (var i : docResult.indicatorResults()) {
					if (Objects.equals(code, i.indicator())) {
						docImpact = i;
						break;
					}
				}
				if (docImpact == null) {
					docImpact = new Ec3IndicatorResult(code, new ArrayList<>());
					docResult.indicatorResults().add(docImpact);
				}
				var value = Ec3Measurement.of(
					impact.amount, impact.indicator.referenceUnit);
				docImpact.values().add(new Ec3ScopeValue(mod.name, value));
			}
		}
		doc.impactResults.addAll(docResults.values());
		return doc;
	}
}

record Validation(String error) {

	static Validation ok() {
		return new Validation(null);
	}

	static Validation error(String message) {
		return new Validation(message);
	}

	boolean hasError() {
		return error != null;
	}

}
