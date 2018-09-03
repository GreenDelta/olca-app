package org.openlca.app.db;

import java.util.HashSet;
import java.util.Set;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.matrix.cache.FlowTypeTable;
import org.openlca.core.matrix.cache.ProcessTable;
import org.openlca.core.model.FlowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LinkingProperties {

	private static final int MAX_SET_SIZE = 50;

	/**
	 * Contains the IDs of product or waste flows that have more than one
	 * provider. If this set is empty, there are no such flows in the database.
	 */
	final Set<Long> multiProviderFlows = new HashSet<>();

	/**
	 * Contains the IDs of processes where product inputs or waste outputs are
	 * __not__ linked to a default provider. If this set is empty, there are no
	 * such unlinked processes in the database.
	 */
	final Set<Long> processesWithoutProviders = new HashSet<>();

	static LinkingProperties check(IDatabase db) {
		LinkingProperties props = new LinkingProperties();
		if (db == null)
			return props;
		FlowTypeTable flowTypes = FlowTypeTable.create(db);
		checkMultiProviders(db, props, flowTypes);
		checkUnlinkedProcesses(db, props, flowTypes);
		return props;
	}

	private static void checkUnlinkedProcesses(IDatabase db,
			LinkingProperties props, FlowTypeTable flowTypes) {
		try {
			String sql = "select f_owner, f_flow, is_input, f_default_provider "
					+ " from tbl_exchanges";
			NativeSql.on(db).query(sql, r -> {
				long flowID = r.getLong(2);
				FlowType type = flowTypes.get(flowID);
				boolean isInput = r.getBoolean(3);
				if (!canHaveProvider(type, isInput))
					return true;
				long providerID = r.getLong(4);
				if (providerID == 0) {
					props.processesWithoutProviders.add(r.getLong(1));
					if (props.processesWithoutProviders.size() > MAX_SET_SIZE)
						return false;
				}
				return true;
			});
		} catch (Exception e) {
			error("Failed to scan exchanges table", e);
		}
	}

	private static void checkMultiProviders(IDatabase db,
			LinkingProperties props, FlowTypeTable flowTypes) {
		ProcessTable processes = ProcessTable.create(db, flowTypes);
		for (LongPair provider : processes.getProviderFlows()) {
			long flowID = provider.getSecond();
			long[] ids = processes.getProviders(flowID);
			if (ids != null && ids.length > 1) {
				props.multiProviderFlows.add(flowID);
				if (props.multiProviderFlows.size() > MAX_SET_SIZE)
					break;
			}
		}
	}

	private static boolean canHaveProvider(FlowType type, boolean isInput) {
		if (isInput && type == FlowType.PRODUCT_FLOW)
			return true;
		if (!isInput && type == FlowType.WASTE_FLOW)
			return true;
		return false;
	}

	private static void error(String message, Exception e) {
		Logger log = LoggerFactory.getLogger(LinkingProperties.class);
		log.error(message, e);
	}
}
