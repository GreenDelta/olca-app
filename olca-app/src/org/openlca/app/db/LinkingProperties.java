package org.openlca.app.db;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.matrix.ProcessProduct;
import org.openlca.core.matrix.cache.FlowTable;
import org.openlca.core.matrix.cache.ProcessTable;
import org.openlca.core.model.FlowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LinkingProperties {

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
		new Check(db).doIt(props);
		return props;
	}

	private static class Check {

		final IDatabase db;
		final FlowTable flowTypes;
		final ProcessTable processes;

		Check(IDatabase db) {
			this.db = db;
			flowTypes = FlowTable.create(db);
			processes = ProcessTable.create(db);
		}

		void doIt(LinkingProperties props) {
			checkMultiProviders(props);
			checkUnlinkedProcesses(props);
		}

		void checkUnlinkedProcesses(LinkingProperties props) {
			try {
				String sql = "select f_owner, f_flow, is_input, f_default_provider "
						+ " from tbl_exchanges";
				NativeSql.on(db).query(sql, r -> {
					long flowID = r.getLong(2);
					FlowType type = flowTypes.type(flowID);
					boolean isInput = r.getBoolean(3);
					if (!canHaveProvider(type, isInput))
						return true;
					long providerID = r.getLong(4);
					if (providerID == 0
							|| processes.getType(providerID) == null) {
						props.processesWithoutProviders.add(r.getLong(1));
					}
					return true;
				});
			} catch (Exception e) {
				error("Failed to scan exchanges table", e);
			}
		}

		void checkMultiProviders(LinkingProperties props) {
			for (ProcessProduct products : processes.getProviders()) {
				long flowID = products.flowId();
				List<ProcessProduct> providers = processes.getProviders(flowID);
				if (providers != null && providers.size() > 1) {
					props.multiProviderFlows.add(flowID);
				}
			}
		}

		boolean canHaveProvider(FlowType type, boolean isInput) {
			if (isInput && type == FlowType.PRODUCT_FLOW)
				return true;
			if (!isInput && type == FlowType.WASTE_FLOW)
				return true;
			return false;
		}

		void error(String message, Exception e) {
			Logger log = LoggerFactory.getLogger(Check.class);
			log.error(message, e);
		}

	}
}
