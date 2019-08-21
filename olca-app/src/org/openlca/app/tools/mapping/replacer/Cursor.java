package org.openlca.app.tools.mapping.replacer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.UncertaintyType;
import org.openlca.io.maps.FlowMapEntry;
import org.openlca.io.maps.FlowRef;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Cursor extends UpdatableCursor {

	static final byte EXCHANGES = 0;
	static final byte IMPACTS = 1;

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final byte type;
	private final Replacer replacer;

	final Stats stats = new Stats();

	Cursor(byte type, Replacer replacer) {
		super(replacer.db);
		this.type = type;
		this.replacer = replacer;
	}

	@Override
	boolean next(ResultSet cursor, PreparedStatement update) {
		long flowID = -1;
		try {
			flowID = cursor.getLong(1);
			FlowMapEntry entry = replacer.entries.get(flowID);
			if (entry == null)
				return false;
			Flow source = replacer.flows.get(entry.sourceFlow.flow.id);
			Flow target = replacer.flows.get(entry.targetFlow.flow.id);
			if (source == null || target == null)
				return false;

			double factor = type == EXCHANGES
					? replacer.factors.forExchange(entry, cursor)
					: replacer.factors.forImpact(entry, cursor);

			// f_flow
			update.setLong(1, target.id);

			// f_unit
			update.setLong(2, entry.targetFlow.unit.id);

			// f_flow_property_factor
			FlowPropertyFactor targetPropFactor = propFactor(
					target, entry.targetFlow);
			update.setLong(3, targetPropFactor.id);

			// amount
			double amount = cursor.getDouble(4);
			update.setDouble(4, factor * amount);

			// resulting_amount_formula
			String formula = cursor.getString(5);
			if (Strings.nullOrEmpty(formula)) {
				update.setString(5, null);
			} else {
				update.setString(5,
						Double.toString(factor) + "* (" + formula + ")");
			}

			// uncertainty
			Uncertainty uncertainty = readUncertainty(cursor);
			updateUncertainty(update, factor, uncertainty);

			update.executeUpdate();
			stats.inc(source.id, Stats.REPLACEMENT);
			return true;
		} catch (Exception e) {
			stats.inc(flowID, Stats.FAILURE);
			return false;
		}
	}

	@Override
	String querySQL() {
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

	@Override
	String updateSQL() {
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
				+ "parameter3_value = ? ";
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
}