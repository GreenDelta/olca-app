package org.openlca.app.tools.mapping.replacer;

import java.sql.ResultSet;
import java.util.Map;

import org.openlca.core.database.IDatabase;
import org.openlca.core.matrix.cache.ConversionTable;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.Unit;
import org.openlca.io.maps.FlowMapEntry;
import org.openlca.io.maps.FlowRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculates the conversion factors for exchanges and characterization factors.
 */
class FactorProvider {

	private final Map<Long, Flow> flows;
	private final ConversionTable conversions;

	FactorProvider(IDatabase db, Map<Long, Flow> flows) {
		this.flows = flows;
		conversions = ConversionTable.create(db);
	}

	double forExchange(FlowMapEntry entry, ResultSet cursor) {
		return getFactor(entry, cursor, true);
	}

	double forImpact(FlowMapEntry entry, ResultSet cursor) {
		return getFactor(entry, cursor, false);
	}

	private double getFactor(FlowMapEntry entry, ResultSet cursor,
			boolean forExchange) {
		try {
			Flow flow = flows.get(entry.sourceFlow().flow.id);
			long propFacID = cursor.getLong("f_flow_property_factor");
			FlowPropertyFactor propFactor = propFactor(flow, propFacID);
			long unitID = cursor.getLong("f_unit");
			Unit unit = unit(propFactor, unitID);
			if (unit == null)
				return 0;

			double factor = forExchange
					? entry.factor()
					: 1 / entry.factor();
			if (propFactor.flowProperty.id == entry.sourceFlow().property.id
					&& unit.id == entry.sourceFlow().unit.id)
				return factor;

			// calculate an additional factor y that converts the exchange
			// property pi and unit ui to the property pj and unit uj of the
			// conversion entry:
			double pi = conversions.getPropertyFactor(propFactor.id);
			double ui = conversions.getUnitFactor(unit.id);
			FlowPropertyFactor entryPropFactor = propFactor(
					flow, entry.sourceFlow());
			if (entryPropFactor == null)
				return 0;
			double pj = conversions.getPropertyFactor(entryPropFactor.id);
			double uj = conversions.getUnitFactor(
					entry.sourceFlow().unit.id);
			double y = forExchange
					? (ui * pj) / (pi * uj)
					: (pi * uj) / (ui * pj);
			return factor * y;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to calculate conversion factor for " + entry, e);
			return 0;
		}
	}

	private FlowPropertyFactor propFactor(Flow flow, long factorID) {
		if (flow == null)
			return null;
		for (FlowPropertyFactor f : flow.flowPropertyFactors) {
			if (f.id == factorID)
				return f;
		}
		return null;
	}

	private FlowPropertyFactor propFactor(Flow flow, FlowRef ref) {
		if (flow == null)
			return null;
		for (FlowPropertyFactor f : flow.flowPropertyFactors) {
			if (f.flowProperty == null)
				continue;
			if (f.flowProperty.id == ref.property.id)
				return f;
		}
		return null;
	}

	private Unit unit(FlowPropertyFactor propFactor, long unitID) {
		if (propFactor == null || propFactor.flowProperty == null)
			return null;
		FlowProperty property = propFactor.flowProperty;
		if (property.unitGroup == null)
			return null;
		for (Unit u : property.unitGroup.units) {
			if (u.id == unitID)
				return u;
		}
		return null;
	}

}
