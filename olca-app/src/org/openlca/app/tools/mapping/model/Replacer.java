package org.openlca.app.tools.mapping.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.openlca.app.db.Database;
import org.openlca.app.tools.mapping.model.FlowMapEntry.SyncState;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.matrix.cache.ConversionTable;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.UncertaintyType;
import org.openlca.core.model.Unit;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Replacer implements Runnable {

	// constants for the statistics
	private final int REPLACEMENT = 0;
	private final int FAILURE = 1;

	private final ReplacerConfig conf;
	private final IDatabase db;
	private final Logger log = LoggerFactory.getLogger(getClass());

	// the valid entries that could be applied: source flow ID -> mapping.
	private final HashMap<Long, FlowMapEntry> entries = new HashMap<>();
	// the source and target flows in the database: flow ID -> flow.
	private final HashMap<Long, Flow> flows = new HashMap<>();

	private ConversionTable conversions;

	public Replacer(ReplacerConfig conf) {
		this.conf = conf;
		this.db = Database.get();
	}

	@Override
	public void run() {
		if (conf == null || (!conf.processes && !conf.methods)) {
			log.info("no configuration; nothing to replace");
			return;
		}

		buildIndices();
		if (entries.isEmpty()) {
			log.info("found no flows that can be mapped");
			return;
		}
		log.info("found {} flows that can be mapped", entries.size());

		try {

			log.info("start updatable cursors");
			Cursor exchangeCursor = null;
			Cursor impactCursor = null;
			ExecutorService pool = Executors.newFixedThreadPool(4);
			if (conf.processes) {
				exchangeCursor = new Cursor(Cursor.EXCHANGES);
				pool.execute(exchangeCursor);
			}
			if (conf.methods) {
				impactCursor = new Cursor(Cursor.IMPACTS);
				pool.execute(impactCursor);
			}
			pool.shutdown();
			int i = 0;
			while (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
				i++;
				log.info("waiting for cursors to finish; {} seconds", i * 10);
			}
			log.info("cursors finished");

			// TODO: replace possible
			if (conf.processes && conf.methods && conf.deleteMapped) {
				// TODO: delete the flows with no failures
			}
			// TODO: log the mapping statistics
		} catch (Exception e) {
			log.error("Flow replacement failed", e);
		}
	}

	private void buildIndices() {
		FlowDao dao = new FlowDao(db);
		for (FlowMapEntry entry : conf.mapping.entries) {

			// only do the replacement for matched mapping entries
			if (entry.syncState != SyncState.MATCHED)
				continue;

			// sync the source flow
			Flow source = dao.getForRefId(entry.sourceFlow.flow.refId);
			if (source == null) {
				entry.syncState = SyncState.UNFOUND_SOURCE;
				continue;
			}
			if (!entry.sourceFlow.syncWith(source)) {
				entry.syncState = SyncState.INVALID_SOURCE;
				continue;
			}

			// sync the target flow
			Optional<Flow> tOpt = conf.provider.persist(entry.targetFlow, db);
			if (!tOpt.isPresent()) {
				entry.syncState = SyncState.UNFOUND_TARGET;
				continue;
			}
			Flow target = tOpt.get();
			if (!entry.targetFlow.syncWith(target)) {
				entry.syncState = SyncState.INVALID_TARGET;
				continue;
			}

			entries.put(source.id, entry);
			flows.put(source.id, source);
			flows.put(target.id, target);
		}
		conversions = ConversionTable.create(db);
	}

	class Stats {
		int failures;
		int replacements;
	}

	private class Cursor implements Runnable {

		static final int EXCHANGES = 0;
		static final int IMPACTS = 1;

		final int type;

		// collected statistics
		final Stats stats = new Stats();
		final HashMap<Long, Stats> flowStats = new HashMap<>();

		Cursor(int type) {
			this.type = type;
		}

		public void run() {
			try {

				Connection con = db.createConnection();
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
					FlowMapEntry entry = entries.get(flowID);
					if (entry == null)
						continue;
					Flow source = flows.get(entry.sourceFlow.flow.id);
					Flow target = flows.get(entry.targetFlow.flow.id);
					if (source == null || target == null)
						continue;

					// check flow property and unit of the source flow
					FlowPropertyFactor propFactor = propFactor(
							source, cursor.getLong("f_flow_property_factor"));
					if (propFactor == null) {
						incStats(source.id, FAILURE);
						continue;
					}
					Unit unit = unit(propFactor.flowProperty, cursor.getLong("f_unit"));
					if (unit == null) {
						incStats(source.id, FAILURE);
						continue;
					}

					double factor = entry.factor;
					if (propFactor.flowProperty.id != entry.sourceFlow.flowProperty.id
							|| unit.id != entry.sourceFlow.unit.id) {
						double pi = conversions.getPropertyFactor(propFactor.id);
						double ui = conversions.getUnitFactor(unit.id);
						double ps = conversions.getPropertyFactor(
								propFactor(source, entry.sourceFlow).id);
						double us = conversions.getUnitFactor(entry.sourceFlow.unit.id);
						factor *= (ui * ps) / (pi * us);
					}

					// check the target flow property
					FlowPropertyFactor targetPropertyFactor = propFactor(
							target, entry.targetFlow);
					if (targetPropertyFactor == null) {
						incStats(source.id, FAILURE);
						continue;
					}

					// amount and formula have type specific names
					double amount = cursor.getDouble(4);
					String formula = cursor.getString(5);
					Uncertainty uncertainty = readUncertainty(cursor);

					update.setLong(1, target.id); // f_flow
					update.setLong(2, entry.targetFlow.unit.id); // f_unit
					update.setLong(3, targetPropertyFactor.id); // f_flow_property_factor
					update.setDouble(4, factor * amount); // resulting_amount_value

					// resulting_amount_formula
					if (Strings.nullOrEmpty(formula)) {
						update.setString(5, null);
					} else {
						update.setString(5,
								Double.toString(factor) + "* (" + formula + ")");
					}

					// uncertainty
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

					update.executeUpdate();
					changed++;
					incStats(source.id, REPLACEMENT);
				}

				cursor.close();
				query.close();
				update.close();
				con.commit();
				con.close();

				log.info("Replaced flows in {} of {} exchanges", changed, total);
			} catch (Exception e) {
				String t = type == EXCHANGES ? "exchanges" : "LCIA factors";
				log.error("Flow replacement in " + t + " failed", e);
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
					+ "resulting_amount_value, "
					+ "resulting_amount_formula, "
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
				if (f.flowProperty.id == ref.flowProperty.id)
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

		private void incStats(long flowID, int type) {
			Stats fstats = flowStats.get(flowID);
			if (fstats == null) {
				fstats = new Stats();
				flowStats.put(flowID, fstats);
			}
			if (type == REPLACEMENT) {
				stats.replacements++;
				fstats.replacements++;
			} else {
				stats.failures++;
				fstats.failures++;
			}
		}
	}
}
