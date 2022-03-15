package org.openlca.app.tools.openepd.input;

import org.openlca.app.tools.openepd.model.EpdDoc;
import org.openlca.app.tools.openepd.model.Ec3Quantity;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.Unit;
import org.openlca.io.UnitMapping;
import org.openlca.util.Strings;

record Quantity(double amount, Unit unit, FlowProperty property) {

	boolean hasUnit() {
		return unit != null && property != null;
	}

	static Quantity detect(EpdDoc epd, IDatabase db) {
		var units = UnitMapping.createDefault(db);
		var q = detect(epd.declaredUnit, units);
		if (q != null && q.hasUnit())
			return q;
		var mass = detect(epd.kgPerDeclaredUnit, units);
		if (mass != null && mass.hasUnit())
			return mass;
		if (q != null)
			return defaultOf(q.amount, units);
		return mass != null
			? defaultOf(mass.amount, units)
			: defaultOf(1, units);
	}

	private static Quantity detect(Ec3Quantity qty, UnitMapping units) {
		if (qty == null || Strings.nullOrEmpty(qty.unit()))
			return null;
			var u = units.getEntry(qty.unit());
			return u == null
				? new Quantity(qty.amount(), null, null)
				: new Quantity(qty.amount(), u.unit, u.flowProperty);
	}

	private static Quantity defaultOf(double amount, UnitMapping units) {
		String defaultUnit = null;
		for (var unit : units.getUnits()) {
			if (unit == null)
				continue;
			switch (unit.trim().toLowerCase()) {
				case "item(s)", "p", "piece":
					defaultUnit = unit;
					break;
				case "kg":
					defaultUnit = unit;
					continue;
				default:
					if (defaultUnit == null) {
						defaultUnit = unit;
					}
			}
		}

		var u = defaultUnit != null
			? units.getEntry(defaultUnit)
			: null;
		return u == null
			? new Quantity(amount, null, null)
			: new Quantity(amount, u.unit, u.flowProperty);
	}
}

