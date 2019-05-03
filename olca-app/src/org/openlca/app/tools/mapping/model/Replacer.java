package org.openlca.app.tools.mapping.model;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Optional;

import org.openlca.app.db.Database;
import org.openlca.app.tools.mapping.model.FlowMapEntry.SyncState;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Replacer implements Runnable {

	private final ReplacerConfig conf;
	private final IDatabase db;
	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * The valid entries that could be applied: source flow ID -> mapping.
	 */
	private final HashMap<Long, FlowMapEntry> entries = new HashMap<>();

	/**
	 * The source and target flows in the database: flow ID -> flow.
	 */
	private final HashMap<Long, Flow> flows = new HashMap<>();

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
			inExchanges();
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
	}

	private void inExchanges() throws Exception {

		Connection con = db.createConnection();
		con.setAutoCommit(false);

		String query = "SELECT "
				+ "f_flow, "
				+ "f_unit, "
				+ "f_flow_property_factor, "
				+ "resulting_amount_value, "
				+ "resulting_amount_formula, "
				+ "distribution_type, "
				+ "parameter1_value, "
				+ "parameter2_value, "
				+ "parameter3_value "

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

		Statement queryStmt = con.createStatement();
		queryStmt.setCursorName("EXCHANGE_CURSOR");
		ResultSet cursor = queryStmt.executeQuery(query);

		String update = "UPDATE tbl_exchanges "
				+ " SET f_flow = ? ,"
				+ " SET f_unit = ? ,"
				+ " SET f_flow_property_factor = ? ,"
				+ " SET resulting_amount_value = ? ,"
				+ " SET resulting_amount_formula = ? ,"
				+ " SET distribution_type = ? ,"
				+ " SET parameter1_value = ? ,"
				+ " SET parameter2_value = ? ,"
				+ " SET parameter3_value = ? "
				+ "WHERE CURRENT OF EXCHANGE_CURSOR";
		Statement updateStmt = con.prepareStatement(update);

		int total = 0;
		int changed = 0;
		while (cursor.next()) {
			total++;

		}

		cursor.close();
		queryStmt.close();
		updateStmt.close();
		con.commit();
		con.close();

		log.info("Replaced flows in {} of {} exchanges", changed, total);
	}
}
