package org.openlca.app.editors.lcia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.LocationDao;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;

class FactorClipboard {

	private final IDatabase db;
	private final List<FlowDescriptor> flows;

	private FactorClipboard() {
		db = Database.get();
		flows = new FlowDao(db).getDescriptors();
	}

	static List<ImpactFactor> read(String text) {
		return new FactorClipboard().parse(text);
	}

	private List<ImpactFactor> parse(String text) {
		if (Strings.nullOrEmpty(text))
			return Collections.emptyList();
		String[] lines = text.split("\n");
		List<ImpactFactor> factors = new ArrayList<>();
		for (String line : lines) {
			String[] row = line.split("\t");
			if (row.length < 3)
				continue;
			if (Strings.nullOrEqual(M.Factor, row[2]))
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
				.filter(d -> Strings.nullOrEqual(d.name, name))
				.map(d -> new FlowDao(db).getForId(d.id))
				.filter(flow -> {
					if (flow.category == null)
						return Strings.nullOrEmpty(category);
					String path = CategoryPath.getFull(flow.category);
					return Strings.nullOrEqual(path, category);
				})
				.collect(Collectors.toList());
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
		try {
			factor.value = Double.parseDouble(amount);
		} catch (Exception e) {
			factor.formula = amount;
		}

		// uncertainty value
		if (row.length > 4) {
			factor.uncertainty = Uncertainty.fromString(row[4]);
		}

		// location
		if (row.length > 5) {
			String code = row[5];
			if (!Strings.nullOrEmpty(code)) {
				LocationDao dao = new LocationDao(db);
				factor.location = dao.getDescriptors()
						.stream()
						.filter(d -> Strings.nullOrEqual(code, d.code))
						.map(d -> dao.getForId(d.id))
						.findFirst()
						.orElse(null);
			}
		}

		return factor;
	}

}
