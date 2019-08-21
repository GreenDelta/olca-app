package org.openlca.app.tools.mapping.replacer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.openlca.core.matrix.cache.ConversionTable;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.UncertaintyType;
import org.openlca.core.model.Unit;
import org.openlca.io.maps.FlowMapEntry;
import org.openlca.io.maps.FlowRef;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Cursor implements Runnable {

	static final byte EXCHANGES = 0;
	static final byte IMPACTS = 1;

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final byte type;
	private final Replacer replacer;

	final Stats stats = new Stats();

	Cursor(byte type, Replacer replacer) {
		this.type = type;
		this.replacer = replacer;
	}

	public void run() {
		try {

			Connection con = replacer.db.createConnection();
			con.setAutoCommit(false);

			Statement query = con.createStatement();
			query.setCursorName(cursorName());
			ResultSet cursor = query.executeQuery(querySQL());
			PreparedStatement update = con.prepareStatement(updateSQL());

			int total = 0;
			int changed = 0;
			while (cursor.next()) {
				total++;

				long flowID = cursor.getLong(1);
				FlowMapEntry entry = replacer.entries.get(flowID);
				if (entry == null)
					continue;
				Flow source = replacer.flows.get(entry.sourceFlow.flow.id);
				Flow target = replacer.flows.get(entry.targetFlow.flow.id);
				if (source == null || target == null)
					continue;

				// check flow property and unit of the source flow
				FlowPropertyFactor propFactor = propFactor(
						source, cursor.getLong("f_flow_property_factor"));
				if (propFactor == null) {
					stats.inc(source.id, Stats.FAILURE);
					continue;
				}
				Unit unit = unit(propFactor.flowProperty, cursor.getLong("f_unit"));
				if (unit == null) {
					stats.inc(source.id, Stats.FAILURE);
					continue;
				}

				// calculate the conversion factor; note that the factor
				// has the inverse meaning for exchanges than for LCIA factors
				double factor = type == EXCHANGES
						? entry.factor
						: 1 / entry.factor;
				if (propFactor.flowProperty.id != entry.sourceFlow.property.id
						|| unit.id != entry.sourceFlow.unit.id) {
					ConversionTable ct = replacer.conversions;
					double pi = ct.getPropertyFactor(propFactor.id);
					double ui = ct.getUnitFactor(unit.id);
					double ps = ct.getPropertyFactor(
							propFactor(source, entry.sourceFlow).id);
					double us = ct.getUnitFactor(entry.sourceFlow.unit.id);
					double y = (ui * ps) / (pi * us);
					factor *= type == EXCHANGES ? y : 1 / y;
				}

				// check the target flow property
				FlowPropertyFactor targetPropertyFactor = propFactor(
						target, entry.targetFlow);
				if (targetPropertyFactor == null) {
					stats.inc(source.id, Stats.FAILURE);
					continue;
				}

				// amount and formula have type specific names
				double amount = cursor.getDouble(4);
				String formula = cursor.getString(5);
				Uncertainty uncertainty = readUncertainty(cursor);

				update.setLong(1, target.id); // f_flow
				update.setLong(2, entry.targetFlow.unit.id); // f_unit
				update.setLong(3, targetPropertyFactor.id); // f_flow_property_factor
				update.setDouble(4, factor * amount); // value

				// resulting_amount_formula
				if (Strings.nullOrEmpty(formula)) {
					update.setString(5, null);
				} else {
					update.setString(5,
							Double.toString(factor) + "* (" + formula + ")");
				}

				// uncertainty
				updateUncertainty(update, factor, uncertainty);

				update.executeUpdate();
				changed++;
				stats.inc(source.id, Stats.REPLACEMENT);
			}

			cursor.close();
			query.close();
			update.close();
			con.commit();
			con.close();

			log.info("{} replaced flows in {} of {} rows",
					cursorName(), changed, total);
		} catch (Exception e) {
			log.error("Flow replacement in " + cursorName() + " failed", e);
			stats.inc(0, Stats.FAILURE);
		}
	}

	private String cursorName() {
		return type == EXCHANGES
				? "EXCHANGE_CURSOR"
				: "IMPACT_CURSOR";
	}

	private String querySQL() {
		String table;
		String value;
		String formula;
		if (type == EXCHANGES) {
			table = "tbl_exchanges";
			value = "resulting_amount_value";
			formula = "resulting_amount_formula";
		} else {
			table = "tbl_impact_factors";
			value = "value";
			formula = "formula";
		}
		return "SELECT "
				+ "f_flow, "
				+ "f_unit, "
				+ "f_flow_property_factor, "
				+ value + " , "
				+ formula + ", "
				+ "distribution_type, "
				+ "parameter1_value, "
				+ "parameter2_value, "
				+ "parameter3_value "
				+ "FROM " + table + " "
				+ "FOR UPDATE OF "
				+ "f_flow, "
				+ "f_unit, "
				+ "f_flow_property_factor, "
				+ value + " , "
				+ formula + ", "
				+ "distribution_type, "
				+ "parameter1_value, "
				+ "parameter2_value, "
				+ "parameter3_value";
	}

	private String updateSQL() {
		String table;
		String value;
		String formula;
		if (type == EXCHANGES) {
			table = "tbl_exchanges";
			value = "resulting_amount_value";
			formula = "resulting_amount_formula";
		} else {
			table = "tbl_impact_factors";
			value = "value";
			formula = "formula";
		}
		return "UPDATE " + table + " "
				+ "SET f_flow = ? , "
				+ "f_unit = ? , "
				+ "f_flow_property_factor = ? , "
				+ value + " = ? , "
				+ formula + " = ? , "
				+ "distribution_type = ? , "
				+ "parameter1_value = ? , "
				+ "parameter2_value = ? , "
				+ "parameter3_value = ? "
				+ "WHERE CURRENT OF " + cursorName();
	}

	private Uncertainty readUncertainty(ResultSet cursor) throws Exception {
		int idx = cursor.getInt("distribution_type");
		if (cursor.wasNull())
			return null;
		switch (UncertaintyType.values()[idx]) {
		case LOG_NORMAL:
			return Uncertainty.logNormal(
					cursor.getDouble("parameter1_value"),
					cursor.getDouble("parameter2_value"));
		case NORMAL:
			return Uncertainty.normal(
					cursor.getDouble("parameter1_value"),
					cursor.getDouble("parameter2_value"));
		case TRIANGLE:
			return Uncertainty.triangle(
					cursor.getDouble("parameter1_value"),
					cursor.getDouble("parameter2_value"),
					cursor.getDouble("parameter3_value"));
		case UNIFORM:
			return Uncertainty.uniform(
					cursor.getDouble("parameter1_value"),
					cursor.getDouble("parameter2_value"));
		default:
			return null;
		}
	}

	private void updateUncertainty(PreparedStatement update,
			double factor, Uncertainty uncertainty) throws SQLException {
		if (uncertainty == null) {
			update.setNull(6, Types.INTEGER);
			update.setNull(7, Types.DOUBLE);
			update.setNull(8, Types.DOUBLE);
			update.setNull(9, Types.DOUBLE);
		} else {
			uncertainty.scale(factor);
			update.setInt(6, uncertainty.distributionType.ordinal());
			update.setDouble(7, uncertainty.parameter1);
			update.setDouble(8, uncertainty.parameter2);
			if (uncertainty.parameter3 != null) {
				update.setDouble(9, uncertainty.parameter3);
			}
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

	private Unit unit(FlowProperty property, long unitID) {
		if (property == null || property.unitGroup == null)
			return null;
		for (Unit u : property.unitGroup.units) {
			if (u.id == unitID)
				return u;
		}
		return null;
	}

}