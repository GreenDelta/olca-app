package org.openlca.app.tools.openepd.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.openlca.app.tools.openepd.model.Ec3Epd;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowResult;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Unit;
import org.openlca.util.Strings;

class Util {

	private Util() {
	}

	static FlowResult initQuantitativeReference(Ec3Epd epd, IDatabase db) {
		var f = new FlowResult();
		f.flow = new Flow();
		f.flow.refId = UUID.randomUUID().toString();
		f.isInput = false;
		f.flow.flowType = FlowType.PRODUCT_FLOW;
		f.flow.name = epd.productName;
		f.flow.description = epd.productDescription;
		var quantity = Quantity.detect(epd, db);
		f.amount = quantity.amount();
		if (quantity.hasUnit()) {
			f.unit = quantity.unit();
			setFlowProperty(f, quantity.property());
		}
		return f;
	}

	static void setFlowProperty(FlowResult r, FlowProperty property) {
		if (r == null || r.flow == null)
			return;
		r.flowPropertyFactor = null;
		var f = r.flow;
		f.flowPropertyFactors.clear();
		f.referenceFlowProperty = null;
		if (property == null)
			return;

		var factor = new FlowPropertyFactor();
		factor.conversionFactor = 1.0;
		factor.flowProperty = property;
		f.flowPropertyFactors.add(factor);
		f.referenceFlowProperty = property;

		r.flowPropertyFactor = factor;
		r.unit = property.getReferenceUnit();
	}

	static List<Unit> allowedUnitsOf(FlowResult r) {
		if (r == null || r.flowPropertyFactor == null)
			return Collections.emptyList();
		var prop = r.flowPropertyFactor.flowProperty;
		return prop == null || prop.unitGroup == null
			? Collections.emptyList()
			: prop.unitGroup.units;
	}

	static String categoryOf(Ec3Epd epd) {
		if (epd == null || epd.productClasses.isEmpty())
			return "";
		var first = epd.productClasses.stream()
			.map(p -> p.second)
			.filter(Strings::notEmpty)
			.findAny()
			.orElse(null);
		if (first == null)
			return "";

		var segments = new ArrayList<String>();
		var word = new StringBuilder();
		Runnable nextWord = () -> {
			if (word.length() == 0)
				return;
			segments.add(word.toString());
			word.setLength(0);
		};

		for (int i = 0; i < first.length(); i++) {
			char c = first.charAt(i);
			if (Character.isLetter(c)
				|| Character.isDigit(c)
				|| c == '&'
				|| c == '-') {
				word.append(c);
				continue;
			}
			nextWord.run();
		}
		nextWord.run();

		return String.join("/", segments);
	}
}
