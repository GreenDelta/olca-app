package org.openlca.app.tools.mapping.replacer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.openlca.core.model.ModelType;
import org.openlca.io.maps.FlowMapEntry;

class AllocationCursor extends UpdatableCursor {

	private final Replacer replacer;

	public AllocationCursor(Replacer replacer) {
		super(replacer.db, ModelType.PROCESS);
		this.replacer = replacer;
	}

	@Override
	void next(ResultSet cursor, PreparedStatement update) {
		long flowID = -1;
		try {

			// select the mapping entry if exists
			flowID = cursor.getLong("f_product");
			FlowMapEntry entry = replacer.entries.get(flowID);
			if (entry == null)
				return;

			// check whether we should update the link
			long processID = cursor.getLong("f_process");
			if (!replacer.processes.contains(processID))
				return;

			update.setLong(1, entry.targetFlow().flow.id);

			update.executeUpdate();
			stats.inc(flowID, Stats.REPLACEMENT);
			updatedModels.add(processID);
		} catch (Exception e) {
			stats.inc(flowID, Stats.FAILURE);
		}
	}

	@Override
	String querySQL() {
		return "SELECT "
				+ " f_process,"
				+ " f_product"
				+ " FROM tbl_allocation_factors"
				+ " FOR UPDATE OF f_product";
	}

	@Override
	String updateSQL() {
		return "UPDATE tbl_allocation_factors"
				+ " SET f_product = ? ";
	}

}
