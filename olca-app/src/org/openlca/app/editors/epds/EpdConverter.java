package org.openlca.app.editors.epds;

import org.openlca.core.model.Epd;
import org.openlca.util.Strings;

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
