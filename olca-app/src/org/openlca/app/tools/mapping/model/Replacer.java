package org.openlca.app.tools.mapping.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.openlca.app.db.Database;
import org.openlca.app.tools.mapping.model.FlowMapEntry.SyncState;
import org.openlca.app.util.Labels;
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

			// waiting for the cursors to finish
			pool.shutdown();
			int i = 0;
			while (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
				i++;
				log.info("waiting for cursors to finish; {} seconds", i * 10);
			}
			log.info("cursors finished");

			// TODO when products were replaced we also need to check
			// whether these products are used in the quant. ref. of
			// product systems and project variants and convert the
			// amounts there.
			// TODO also: we need to replace wuch flows in allocation
			// factors; the application of the conversion factor is
			// not required there.

			// collect and log statistics
			Stats stats = new Stats();
			if (exchangeCursor != null) {
				stats.add(exchangeCursor.stats);
				exchangeCursor.stats.log("exchanges", flows);
			}
			if (impactCursor != null) {
				stats.add(impactCursor.stats);
				impactCursor.stats.log("impacts", flows);
			}

			// update the mapping entries
			for (Long flowID : entries.keySet()) {
				FlowMapEntry e = entries.get(flowID);
				if (flowID == null || e == null)
					continue;
				if (stats.hadFailures(flowID)) {
					e.syncState = SyncState.INVALID_SOURCE;
					e.syncMessage = "Replacement error";
				} else {
					e.syncState = SyncState.APPLIED;
					e.syncMessage = "Applied";
				}
			}

			// delete mapped flows
			if (!conf.deleteMapped || !conf.processes || !conf.methods)
				return;
			if (stats.failures > 0) {
				log.warn("Will not delete mapped flows because"
						+ " there were {} failures in replacement process",
						stats.failures);
				return;
			}

			// TODO: collect the IDs of the used flows
			// delete mapped and unused flows

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

	private static class Stats {
		private static final byte REPLACEMENT = 0;
		private static final byte FAILURE = 1;

		int failures;
		int replacements;
		final HashMap<Long, Integer> flowFailures = new HashMap<>();
		final HashMap<Long, Integer> flowReplacements = new HashMap<>();

		void add(Stats s) {
			if (s == null)
				return;
			failures += s.failures;
			replacements += s.replacements;
			for (Long flowID : s.flowFailures.keySet()) {
				int c = s.flowFailures.getOrDefault(flowID, 0);
				flowFailures.put(flowID,
						c + flowFailures.getOrDefault(flowID, 0));
			}
			for (Long flowID : s.flowReplacements.keySet()) {
				int c = s.flowReplacements.getOrDefault(flowID, 0);
				flowReplacements.put(flowID,
						c + flowReplacements.getOrDefault(flowID, 0));
			}
		}

		boolean hadFailures(long flowID) {
			Integer failures = flowFailures.get(flowID);
			if (failures == null)
				return false;
			return failures > 0;
		}

		void inc(long flowID, byte type) {
			HashMap<Long, Integer> flowStats;
			if (type == REPLACEMENT) {
				replacements++;
				flowStats = flowReplacements;
			} else {
				failures++;
				flowStats = flowFailures;
			}
			Integer count = flowStats.get(flowID);
			if (count == null) {
				flowStats.put(flowID, 1);
			} else {
				flowStats.put(flowID, count + 1);
			}
		}

		void log(String context, HashMap<Long, Flow> flows) {
			Logger log = LoggerFactory.getLogger(getClass());
			if (replacements == 0 && failures == 0) {
				log.info("No flows replaced in {}", context);
				return;
			}
			if (failures > 0) {
				log.warn("There were failures while replacing flows in {}", context);
			}
			log.info("{} replacements and {} failures in {}",
					replacements, failures, context);
			if (!log.isTraceEnabled())
				return;
			HashSet<Long> ids = new HashSet<>();
			ids.addAll(flowFailures.keySet());
			ids.addAll(flowReplacements.keySet());
			for (Long id : ids) {
				Flow flow = flows.get(id);
				if (flow == null)
					continue;
				int rcount = flowReplacements.getOrDefault(id, 0);
				int fcount = flowFailures.getOrDefault(id, 0);
				if (rcount == 0 && fcount == 0)
					continue;
				log.trace("Flow {} uuid={} :: {} replacements, {} failures in {}",
						Labels.getDisplayName(flow), flow.refId, rcount, fcount, context);
			}
		}
	}

	private class Cursor implements Runnable {

		static final byte EXCHANGES = 0;
		static final byte IMPACTS = 1;

		final byte type;
		final Stats stats = new Stats();

		Cursor(byte type) {
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
						stats.inc(source.id, Stats.FAILURE);
						continue;
					}
					Unit unit = unit(propFactor.flowProperty, cursor.getLong("f_unit"));
					if (unit == null) {
						stats.inc(source.id, Stats.FAILURE);
						continue;
					}

					// calculate the conversion factor; not that the factor
					// has the inverse meaning for exchanges than for LCIA factors
					double factor = type == EXCHANGES
							? entry.factor
							: 1 / entry.factor;
					if (propFactor.flowProperty.id != entry.sourceFlow.flowProperty.id
							|| unit.id != entry.sourceFlow.unit.id) {
						double pi = conversions.getPropertyFactor(propFactor.id);
						double ui = conversions.getUnitFactor(unit.id);
						double ps = conversions.getPropertyFactor(
								propFactor(source, entry.sourceFlow).id);
						double us = conversions.getUnitFactor(entry.sourceFlow.unit.id);
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

	}
}
