package org.openlca.app.editors.results.openepd.input;

import org.openlca.app.editors.results.openepd.model.Ec3Epd;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.Unit;
import org.openlca.io.UnitMapping;
import org.openlca.util.Strings;

record Quantity(double amount, Unit unit, FlowProperty property) {

	boolean hasUnit() {
		return unit != null && property != null;
	}

	static Quantity detect(Ec3Epd epd, IDatabase db) {
		var units = UnitMapping.createDefault(db);
		var q = detect(epd.declaredUnit, units);
		if (q != null && q.hasUnit())
			return q;
		var mass = detect(epd.massPerDeclaredUnit, units);
		if (mass != null && mass.hasUnit())
			return mass;
		if (q != null)
			return defaultOf(q.amount, units);
		return mass != null
			? defaultOf(mass.amount, units)
			: defaultOf(1, units);
	}

	private static Quantity detect(String str, UnitMapping units) {
		if (Strings.nullOrEmpty(str))
			return null;
		var parts = str.split(" ");
		try {
			Double amount = null;
			String unit = null;
			for (var part : parts) {
				var p = part.trim();
				if (p.isEmpty())
					continue;
				if (amount == null) {
					amount = Double.parseDouble(p);
				} else {
					unit = p;
				}
			}

			if (amount == null)
				return null;
			if (unit == null)
				return new Quantity(amount, null, null);

			var u = units.getEntry(unit);
			return u == null
				? new Quantity(amount, null, null)
				: new Quantity(amount, u.unit, u.flowProperty);

		} catch (Exception e) {
			return null;
		}
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

