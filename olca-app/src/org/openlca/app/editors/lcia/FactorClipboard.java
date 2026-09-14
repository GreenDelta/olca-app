package org.openlca.app.editors.lcia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.Numbers;
import org.openlca.commons.Strings;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.LocationDao;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.Location;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.io.CategoryPath;

class FactorClipboard {

	private final IDatabase db;
	private final List<FlowDescriptor> flows;
	private Map<String, Long> locationIds;

	private FactorClipboard() {
		db = Database.get();
		flows = new FlowDao(db).getDescriptors();
	}

	static List<ImpactFactor> read(String text) {
		return new FactorClipboard().parse(text);
	}

	private List<ImpactFactor> parse(String text) {
		if (Strings.isBlank(text))
			return Collections.emptyList();
		String[] lines = text.split("\n");
		List<ImpactFactor> factors = new ArrayList<>();
		for (String line : lines) {
			String[] row = line.split("\t");
			if (row.length < 3)
				continue;
			if (Objects.equals(M.Factor, row[2]))
				continue; // the header row
			ImpactFactor factor = factor(row);
			if (factor != null) {
				factors.add(factor);
			}
		}
		return factors;
	}

	private ImpactFactor factor(String[] row) {
		if (row.length < 4)
			return null;
		String name = row[0];
		String category = row[1];
		String amount = row[2];
		String unit = row[3];

		// filter the flows by matching names and categories
		List<Flow> candidates = flows.stream()
			.filter(d -> Objects.equals(d.name, name))
			.map(d -> new FlowDao(db).getForId(d.id))
			.filter(flow -> {
				if (flow.category == null)
					return Strings.isBlank(category);
				String path = CategoryPath.getFull(flow.category);
				return Objects.equals(path, category);
			})
			.toList();
		if (candidates.isEmpty())
			return null;

		// find a matching flow for the unit
		// the unit in the table has the format:
		// <LCIA ref. unit> / <flow unit>
		// the following only works if the LCIA
		// ref. unit does not contain a slash, but
		// this should be very unlikely
		int i = unit.indexOf('/');
		if (i >= 0) {
			unit = unit.substring(i + 1).trim();
		}
		ImpactFactor factor = new ImpactFactor();
		for (Flow flow : candidates) {
			for (FlowPropertyFactor p : flow.flowPropertyFactors) {
				if (p.flowProperty == null
					|| p.flowProperty.unitGroup == null)
					continue;
				Unit u = p.flowProperty.unitGroup.getUnit(unit);
				if (u == null)
					continue;
				factor.flow = flow;
				factor.flowPropertyFactor = p;
				factor.unit = u;
				if (Objects.equals(p.flowProperty,
					flow.referenceFlowProperty))
					break;
			}
			if (factor.flow != null)
				break;
		}
		if (factor.flow == null)
			return null;

		// set the amount value / formula
		var value = Numbers.tryParseAnyFormat(amount);
		if (value.isPresent()) {
			factor.value = value.getAsDouble();
		} else {
			factor.formula = amount;
		}

		// uncertainty value
		if (row.length > 4) {
			factor.uncertainty = Uncertainty.fromString(row[4]);
		}

		// location
		if (row.length > 5) {
			factor.location = getLocation(row[5]);
		}

		return factor;
	}

	private Location getLocation(String entry) {
		if (Strings.isBlank(entry))
			return null;

		if (locationIds == null) {
			locationIds = new HashMap<>();
			var dao = new LocationDao(db);
			for (var d : dao.getDescriptors()) {
				if (Strings.isNotBlank(d.code)) {
					locationIds.putIfAbsent(keyOf(d.code), d.id);
				}
				if (Strings.isNotBlank(d.name)) {
					locationIds.putIfAbsent(keyOf(d.name), d.id);
				}
			}
		}

		var id = locationIds.get(keyOf(entry));
		return id != null
			? db.get(Location.class, id)
			: null;
	}

	private String keyOf(String s) {
		return s != null
			? s.trim().toLowerCase(Locale.ROOT)
			: "";
	}
}
