package org.openlca.app.tools.mapping.replacer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.openlca.core.model.ModelType;
import org.openlca.io.maps.FlowMapEntry;

class ProductLinkCursor extends UpdatableCursor {

	private final Replacer replacer;

	ProductLinkCursor(Replacer replacer) {
		super(replacer.db, ModelType.PRODUCT_SYSTEM);
		this.replacer = replacer;
	}

	@Override
	boolean next(ResultSet cursor, PreparedStatement update) {
		long flowID = -1;
		try {

			// select the mapping entry if exists
			flowID = cursor.getLong("f_flow");
			FlowMapEntry entry = replacer.entries.get(flowID);
			if (entry == null)
				return false;

			// check whether we should update the link
			long processID = cursor.getLong("f_process");
			if (!replacer.processes.contains(processID))
				return false;

			// select the provider
			long provider = 0L;
			if (entry.targetFlow.provider != null) {
				provider = entry.targetFlow.provider.id;
			} else {
				provider = cursor.getLong("f_provider");
			}

			update.setLong(1, provider);
			update.setLong(2, entry.targetFlow.flow.id);

			long systemID = cursor.getLong("f_product_system");
			stats.inc(entry.sourceFlow.flow.id, Stats.REPLACEMENT);
			updatedModels.add(systemID);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	String querySQL() {
		return "SELECT "
				+ " f_product_system,"
				+ " f_provider,"
				+ " f_flow,"
				+ " f_process"
				+ " FROM tbl_process_links"
				+ " FOR UPDATE OF "
				+ " f_provider, "
				+ " f_flow";
	}

	@Override
	String updateSQL() {
		return "UPDATE tbl_process_links"
				+ " SET f_provider = ? , "
				+ " f_flow = ? ";
	}

}
