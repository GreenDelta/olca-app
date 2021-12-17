package org.openlca.app.tools.mapping.replacer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Set;

import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.UncertaintyType;
import org.openlca.io.maps.FlowMapEntry;
import org.openlca.io.maps.FlowRef;
import org.openlca.util.Strings;

/**
 * Replaces flows and updates the amounts in exchanges and characterization
 * factors.
 */
class AmountCursor extends UpdatableCursor {

	private final Replacer replacer;

	/**
	 * Creates a new amount cursor. For the type, PROCESS or IMPACT_CATEGORY is
	 * allowed.
	 */
	AmountCursor(ModelType type, Replacer replacer) {
		super(replacer.db, type);
		this.replacer = replacer;
	}

	@Override
	void next(ResultSet cursor, PreparedStatement update) {
		long flowID = -1;
		try {

			// select the mapping entry if exists
			flowID = cursor.getLong("f_flow");
			FlowMapEntry entry = replacer.entries.get(flowID);
			if (entry == null)
				return;
			Flow source = replacer.flows.get(entry.sourceFlow().flow.id);
			Flow target = replacer.flows.get(entry.targetFlow().flow.id);
			if (source == null || target == null)
				return;

			// check if we should replace a flow here
			long ownerID = cursor.getLong(1);
			Set<Long> owners = type == ModelType.PROCESS
					? replacer.processes
					: replacer.impacts;
			if (!owners.contains(ownerID))
				return;

			// get the conversion factor
			double factor = type == ModelType.PROCESS
					? replacer.factors.forExchange(entry, cursor)
					: replacer.factors.forImpact(entry, cursor);

			// f_flow
			update.setLong(1, target.id);

			// f_unit
			update.setLong(2, entry.targetFlow().unit.id);

			// f_flow_property_factor
			FlowPropertyFactor targetPropFactor = propFactor(
					target, entry.targetFlow());
			update.setLong(3, targetPropFactor.id);

			// amount
			double amount = cursor.getDouble(5);
			update.setDouble(4, factor * amount);

			// resulting_amount_formula
			String formula = cursor.getString(6);
			if (Strings.nullOrEmpty(formula)) {
				update.setString(5, null);
			} else if (factor == 1) {
				update.setString(5, formula);
			} else {
				update.setString(5,
						Double.toString(factor) + " * (" + formula + ")");
			}

			// uncertainty
			Uncertainty uncertainty = readUncertainty(cursor);
			updateUncertainty(update, factor, uncertainty);

			// f_default_provider
			if (type == ModelType.PROCESS) {
				if (entry.targetFlow().provider == null) {
					update.setNull(10, Types.INTEGER);
				} else {
					update.setLong(10, entry.targetFlow().provider.id);
				}
			}

			update.executeUpdate();
			stats.inc(flowID, Stats.REPLACEMENT);
			updatedModels.add(ownerID);
		} catch (Exception e) {
			stats.inc(flowID, Stats.FAILURE);
		}
	}

	@Override
	String querySQL() {
		String table;
		String owner;
		String value;
		String formula;
		if (type == ModelType.PROCESS) {
			table = "tbl_exchanges";
			owner = "f_owner";
			value = "resulting_amount_value";
			formula = "resulting_amount_formula";
		} else {
			table = "tbl_impact_factors";
			owner = "f_impact_category";
			value = "value";
			formula = "formula";
		}

		String query = "SELECT "
				/* 1 */ + owner + ", "
				/* 2 */ + "f_flow, "
				/* 3 */ + "f_unit, "
				/* 4 */ + "f_flow_property_factor, "
				/* 5 */ + value + " , "
				/* 6 */ + formula + ", "
				/* 7 */ + "distribution_type, "
				/* 8 */ + "parameter1_value, "
				/* 9 */ + "parameter2_value, "
				/* 10 */ + "parameter3_value ";

		if (type == ModelType.PROCESS) {
			query += ", f_default_provider ";
		}

		query += "FROM " + table + " "
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

		if (type == ModelType.PROCESS) {
			query += ", f_default_provider ";
		}

		return query;
	}

	@Override
	String updateSQL() {
		String table;
		String value;
		String formula;
		if (type == ModelType.PROCESS) {
			table = "tbl_exchanges";
			value = "resulting_amount_value";
			formula = "resulting_amount_formula";
		} else {
			table = "tbl_impact_factors";
			value = "value";
			formula = "formula";
		}

		String sql = "UPDATE " + table + " "
		/* 1 */ + "SET f_flow = ? , "
		/* 2 */ + "f_unit = ? , "
		/* 3 */ + "f_flow_property_factor = ? , "
		/* 4 */ + value + " = ? , "
		/* 5 */ + formula + " = ? , "
		/* 6 */ + "distribution_type = ? , "
		/* 7 */ + "parameter1_value = ? , "
		/* 8 */ + "parameter2_value = ? , "
		/* 9 */ + "parameter3_value = ? ";

		if (type == ModelType.PROCESS) {
			/* 10 */ sql += " , f_default_provider = ?";
		}
		return sql;
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
			} else {
				update.setNull(9, Types.DOUBLE);
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