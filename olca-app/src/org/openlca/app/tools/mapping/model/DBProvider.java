package org.openlca.app.tools.mapping.model;

import java.util.Collections;
import java.util.List;

import org.openlca.core.database.IDatabase;

public class DBProvider implements IProvider {

	public final IDatabase db;

	public DBProvider(IDatabase db) {
		this.db = db;
	}

	@Override
	public List<FlowRef> getFlowRefs() {
		return Collections.emptyList();
	}

	@Override
	public void persist(List<FlowRef> refs, IDatabase db) {
	}
}
